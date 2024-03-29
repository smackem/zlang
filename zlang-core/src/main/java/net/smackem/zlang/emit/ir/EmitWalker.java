package net.smackem.zlang.emit.ir;

import com.google.common.base.CharMatcher;
import net.smackem.zlang.lang.ZLangParser;
import net.smackem.zlang.symbols.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class EmitWalker extends ScopeWalker<EmitWalker.Value> {
    private final static Logger log = LoggerFactory.getLogger(EmitWalker.class);
    private final List<Type> types = new ArrayList<>();
    private final List<FunctionSymbol> functions = new ArrayList<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<FunctionSymbol, Instruction> codeMap = new HashMap<>();
    private final FunctionSymbol initFunction;
    private final Set<Register> allocatedRegisters = EnumSet.noneOf(Register.class);
    private final List<Instruction> initInstructions = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final Deque<BlockExprInfo> blockExprInfos = new ArrayDeque<>();
    private List<Instruction> currentInstructions = instructions;
    private FunctionSymbol currentFunction;
    private Register firstVolatileRegister;

    EmitWalker(String moduleName, ProgramStructure programStructure) {
        super(moduleName, programStructure.globalScope(), programStructure.scopes());
        this.initFunction = new FunctionSymbol(Naming.GENERATED_INIT_FUNCTION_PREFIX + moduleName, null, programStructure.globalScope());
        this.functions.add(this.initFunction);
    }

    public EmittedModule buildModule() {
        if (this.initInstructions.isEmpty() == false) {
            this.initInstructions.add(new Instruction(OpCode.Ret));
        }
        this.instructions.addAll(this.initInstructions);
        return new EmittedModule(this.types, this.functions, this.instructions, this.codeMap, this.labels);
    }

    @Override
    public Value visitModule(ZLangParser.ModuleContext ctx) {
        enterScope(ctx);
        super.visitModule(ctx);
        popScope();
        return null;
    }

    @Override
    public Value visitInterfaceDecl(ZLangParser.InterfaceDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitInterfaceDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Value visitStructDecl(ZLangParser.StructDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitStructDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Value visitUnionDecl(ZLangParser.UnionDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitUnionDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Value visitFunctionDecl(ZLangParser.FunctionDeclContext ctx) {
        enterScope(ctx);
        final FunctionSymbol function = (FunctionSymbol) currentScope();
        enterFunction(function, this.instructions);
        this.functions.add(this.currentFunction);
        super.visitFunctionDecl(ctx);
        if (currentFunctionEndsWithReturn() == false) {
            if (this.currentFunction.type() != null) {
                return logLocalError(ctx, "function must return a value");
            }
            emitFunctionCleanup(function);
            emit(function.isEntryPoint() ? OpCode.Halt : OpCode.Ret);
        }
        popScope();
        this.currentFunction = null;
        return null;
    }

    private boolean currentFunctionEndsWithReturn() {
        if (this.currentInstructions.isEmpty()) {
            return false;
        }
        final Instruction instr = this.currentInstructions.get(this.currentInstructions.size() - 1);
        return instr.opCode() == OpCode.Ret || instr.opCode() == OpCode.Halt;
    }

    @Override
    public Value visitGlobalDecl(ZLangParser.GlobalDeclContext ctx) {
        super.visitGlobalDecl(ctx);
        return null;
    }

    @Override
    public Value visitStatement(ZLangParser.StatementContext ctx) {
        super.visitStatement(ctx);
        return null;
    }

    @Override
    public Value visitPanicStmt(ZLangParser.PanicStmtContext ctx) {
        final Value value = ctx.expr().accept(this);
        emit(OpCode.Mov, Register.R000, value.register);
        emit(OpCode.Halt);
        return null;
    }

    @Override
    public Value visitForStmt(ZLangParser.ForStmtContext ctx) {
        enterScope(ctx);
        super.visitForStmt(ctx);
        popScope();
        return null;
    }

    @Override
    public Value visitBlock(ZLangParser.BlockContext ctx) {
        enterScope(ctx);
        super.visitBlock(ctx);
        popScope();
        return null;
    }

    @Override
    public Value visitVarDeclStmt(ZLangParser.VarDeclStmtContext ctx) {
        emitInitAssignment(ctx, ctx.parameter().Ident().getText(), ctx.expr());
        return null;
    }

    @Override
    public Value visitBindingStmt(ZLangParser.BindingStmtContext ctx) {
        emitInitAssignment(ctx, ctx.parameter().Ident().getText(), ctx.expr());
        return null;
    }

    private void emitInitAssignment(ParserRuleContext ctx, String ident, ZLangParser.ExprContext expr) {
        if (this.currentFunction == null) {
            // global var
            enterFunction(this.initFunction, this.initInstructions);
        }
        if (expr != null) {
            emitIdentAssign(ctx, ident, expr, true);
        }
    }

    @Override
    public Value visitAssignStmt(ZLangParser.AssignStmtContext ctx) {
        if (ctx.postFixedPrimary() != null) {
            emitPostFixedPrimaryAssign(ctx.postFixedPrimary(), ctx.expr());
        } else {
            emitIdentAssign(ctx, ctx.Ident().getText(), ctx.expr(), false);
        }
        return null;
    }

    private void emitIdentAssign(ParserRuleContext ctx, String ident, ZLangParser.ExprContext rvalueCtx, boolean init) {
        final VariableSymbol variable = resolveAssignableVariable(ctx, ident, init);
        assert variable != null;
        final Value rvalue = rvalueCtx.accept(this);
        emitIdentAssign(ctx, variable, rvalue);
    }

    private VariableSymbol resolveAssignableVariable(ParserRuleContext ctx, String ident, boolean init) {
        final Symbol symbol = currentScope().resolve(ident);
        if (symbol == null) {
            logLocalError(ctx, "'" + ident + "' is not defined in this context");
            return null;
        }
        if (symbol instanceof VariableSymbol == false) {
            logLocalError(ctx, "'" + ident + "' is not a variable");
            return null;
        }
        final VariableSymbol variable = (VariableSymbol) symbol;
        if (init == false && variable.isAssignable() == false) {
            logLocalError(ctx, "'" + ident + "' is not assignable");
            return null;
        }
        if (variable.isGlobal()) {
            if (variable.type().registerType().isReferenceType() && init == false) {
                final Register r = allocFreedRegister();
                emit(OpCode.ldGlb(variable.type()), r, variable.address());
                emit(OpCode.RemoveRef, r);
                freeRegister(r);
            }
        } else {
            final Register r = Register.fromNumber(variable.address());
            if (variable.type().registerType().isReferenceType()) {
                emit(OpCode.RemoveRef, r);
            }
        }
        return variable;
    }

    private VariableSymbol emitIdentAssign(ParserRuleContext ctx, String ident, Value rvalue, boolean init) {
        final VariableSymbol variable = resolveAssignableVariable(ctx, ident, init);
        assert variable != null;
        return emitIdentAssign(ctx, variable, rvalue);
    }

    private VariableSymbol emitIdentAssign(ParserRuleContext ctx, VariableSymbol variable, Value rvalue) {
        if (Types.isAssignable(variable.type(), rvalue.type) == false) {
            logLocalError(ctx, "incompatible types in assignment");
            return null;
        }
        if (Scopes.enclosingModule(variable.definingScope()) != Scopes.enclosingModule(currentScope())) {
            logLocalError(ctx, "access to foreign variables not allowed");
            return null;
        }
        if (variable.isGlobal()) {
            emit(OpCode.stGlb(variable.type()), rvalue.register, variable.address());
        } else {
            final Register r = Register.fromNumber(variable.address());
            emit(OpCode.Mov, r, rvalue.register);
        }
        if (rvalue.type.registerType().isReferenceType()) {
            emit(OpCode.AddRef, rvalue.register);
        }
        freeRegister(rvalue.register);
        return variable;
    }

    public void emitPostFixedPrimaryAssign(ZLangParser.PostFixedPrimaryContext ctx, ZLangParser.ExprContext exprCtx) {
        Value primary = ctx.primary() != null
                ? ctx.primary().accept(this)
                : ctx.postFixedPrimary().accept(this);

        if (ctx.arrayAccessPostfix() != null) {
            if (primary.type instanceof ListType listType) {
                final Register arrayRegister = allocFreedRegister(primary.register);
                emit(OpCode.ldFld(listType.arrayType()), arrayRegister, primary.register, listType.arrayField().address());
                primary = value(arrayRegister, listType.arrayType());
            }
            if (primary.type instanceof ArrayType == false) {
                logLocalError(ctx, "indexed value is not an array");
                return;
            }
            final Value index = ctx.arrayAccessPostfix().expr().accept(this);
            if (Types.isEffectivelyInteger(index.type) == false) {
                logLocalError(ctx, "index is not of type int");
                return;
            }
            final ArrayType arrayType = (ArrayType) primary.type;
            if (arrayType.elementType().registerType().isReferenceType()) {
                final Register r = allocFreedRegister();
                emit(OpCode.ldElem(arrayType.elementType()), r, primary.register, index.register);
                emit(OpCode.RemoveRef, r);
                freeRegister(r);
            }
            final Value rvalue = exprCtx.accept(this);
            if (Types.isAssignable(arrayType.elementType(), rvalue.type) == false) {
                logLocalError(ctx, "incompatible types in assignment");
                return;
            }
            if (arrayType.elementType().registerType().isReferenceType()) {
                emit(OpCode.AddRef, rvalue.register);
            }
            emit(OpCode.stElem(arrayType.elementType()), rvalue.register, primary.register, index.register);
            freeRegister(rvalue.register, primary.register, index.register);
            return;
        }

        if (ctx.fieldAccessPostfix() != null) {
            if (primary.type instanceof StructSymbol == false) {
                logLocalError(ctx, "field target is not a struct");
                return;
            }
            final MemberScope memberScope = (MemberScope) primary.type;
            if (Scopes.enclosingModule(memberScope) != Scopes.enclosingModule(currentScope())) {
                logLocalError(ctx, "access to field of foreign type not allowed");
                return;
            }
            final Symbol field = memberScope.resolveMember(ctx.fieldAccessPostfix().Ident().getText());
            if (field instanceof FieldSymbol == false) {
                logLocalError(ctx, "field id does not refer to a field");
                return;
            }
            if (field.type().registerType().isReferenceType()) {
                final Register r = allocFreedRegister();
                emit(OpCode.ldFld(field.type()), r, primary.register, field.address());
                emit(OpCode.RemoveRef, r);
                freeRegister(r);
            }
            final Value rvalue = exprCtx.accept(this);
            if (Types.isAssignable(field.type(), rvalue.type) == false) {
                logLocalError(ctx, "incompatible types in assignment");
                return;
            }
            if (field.type().registerType().isReferenceType()) {
                emit(OpCode.AddRef, rvalue.register);
            }
            emit(OpCode.stFld(field.type()), rvalue.register, primary.register, field.address());
            freeRegister(rvalue.register, primary.register);
            return;
        }

        throw new UnsupportedOperationException("unsupported postFixedPrimary");
    }

    @Override
    public Value visitReturnStmt(ZLangParser.ReturnStmtContext ctx) {
        final boolean returnsReference;
        if (ctx.expr() == null) {
            if (this.currentFunction.type() != null) {
                return logLocalError(ctx, "function signature requires a return value");
            }
            returnsReference = false;
        } else {
            if (this.currentFunction.type() == null) {
                return logLocalError(ctx, "function signature does not allow a return value");
            }
            final Value value = ctx.expr().accept(this);
            if (Types.isAssignable(this.currentFunction.type(), value.type()) == false) {
                return logLocalError(ctx, "incompatible return type. expecting %s, got %s".formatted(
                        this.currentFunction.type(), value.type()));
            }
            emit(OpCode.Mov, Register.R000, value.register);
            returnsReference = value.type().registerType().isReferenceType();
            if (returnsReference) {
                emit(OpCode.AddRef, Register.R000);
            }
            freeRegister(value.register);
        }
        emitFunctionCleanup(this.currentFunction);
        if (returnsReference) {
            emit(OpCode.RemoveRef, Register.R000);
        }
        emit(this.currentFunction.isEntryPoint() ? OpCode.Halt : OpCode.Ret);
        return null;
    }

    @Override
    public Value visitInvocationStmt(ZLangParser.InvocationStmtContext ctx) {
        final Value retVal = super.visitInvocationStmt(ctx);
        if (retVal != null) {
            freeRegister(retVal.register); // discard return value
        }
        return null;
    }

    @Override
    public Value visitMethodInvocation(ZLangParser.MethodInvocationContext ctx) {
        final Value primary = ctx.postFixedPrimary().accept(this);
        final Value retVal = emitMethodInvocation(primary, ctx.methodInvocationPostfix());
        if (retVal != null) {
            freeRegister(retVal.register); // discard return value
        }
        return null;
    }

    @Override
    public Value visitWhileStmt(ZLangParser.WhileStmtContext ctx) {
        final int conditionIndex = this.currentInstructions.size();
        final Label loopLabel = addLabel();
        final Label exitLabel = addLabel();
        final Value condition = ctx.expr().accept(this);
        if (condition.type != BuiltInType.BOOL.type()) {
            return logLocalError(ctx, "while condition is not of type bool, but " + condition.type);
        }
        loopLabel.setTarget(this.currentInstructions.get(conditionIndex));
        emitBranch(condition.register, exitLabel);
        freeRegister(condition.register);
        ctx.block().accept(this);
        emit(OpCode.Collect);
        emitBranch(loopLabel);
        exitLabel.setTarget(emitNop());
        return null;
    }

    @Override
    public Value visitIfStmt(ZLangParser.IfStmtContext ctx) {
        final Label exitLabel = addLabel();
        emitIfBranch(ctx.expr(), ctx.block(), exitLabel);
        for (final var elseIf : ctx.elseIfClause()) {
            emitIfBranch(elseIf.expr(), elseIf.block(), exitLabel);
        }
        if (ctx.elseClause() != null) {
            ctx.elseClause().block().accept(this);
        }
        exitLabel.setTarget(emitNop());
        return null;
    }

    private void emitIfBranch(ZLangParser.ExprContext expr, ZLangParser.BlockContext block, Label exitLabel) {
        final Label skipLabel = addLabel();
        final Value condition = expr.accept(this);
        if (condition.type != BuiltInType.BOOL.type()) {
            logLocalError(expr, "if condition is not of type bool, but " + condition.type);
            return;
        }
        emitBranch(condition.register, skipLabel);
        freeRegister(condition.register);
        block.accept(this);
        emitBranch(exitLabel);
        skipLabel.setTarget(emitNop());
    }

    @Override
    public Value visitForRangeStmt(ZLangParser.ForRangeStmtContext ctx) {
        final List<ZLangParser.ExprContext> rangeExpr = ctx.range().expr();
        final Value from = rangeExpr.get(0).accept(this);
        final Value to;
        final Value step;
        if (rangeExpr.size() >= 3) {
            to = rangeExpr.get(2).accept(this);
            step = rangeExpr.get(1).accept(this);
        } else {
            to = rangeExpr.get(1).accept(this);
            final Register r = allocFreedRegister();
            emit(OpCode.Ldc_i32, r, 1);
            step = value(r, BuiltInType.INT.type());
        }
        if (Types.isEffectivelyInteger(from.type) == false) {
            return logLocalError(ctx, "lower bound in for statement must be of type int");
        }
        if (Types.isEffectivelyInteger(to.type) == false) {
            return logLocalError(ctx, "upper bound in for statement must be of type int");
        }
        if (Types.isEffectivelyInteger(step.type) == false) {
            return logLocalError(ctx, "step in for statement must be of type int");
        }
        final VariableSymbol index = emitIdentAssign(ctx.parameter(), ctx.parameter().Ident().getText(), from, true);
        assert index != null && index.isGlobal() == false;
        final Register indexRegister = Register.fromNumber(index.address());
        final Label loopLabel = addLabel();
        final Label exitLabel = addLabel();
        final Register condRegister = allocFreedRegister();
        emit(OpCode.Lt_i32, condRegister, indexRegister, to.register);
        loopLabel.setTarget(this.currentInstructions.get(this.currentInstructions.size() - 1));
        emitBranch(condRegister, exitLabel);
        ctx.block().accept(this);
        emit(OpCode.Collect);
        emit(OpCode.Add_i32, indexRegister, indexRegister, step.register);
        emitBranch(loopLabel);
        exitLabel.setTarget(emitNop());
        freeRegister(from.register, to.register, step.register);
        return null;
    }

    @Override
    public Value visitForIteratorStmt(ZLangParser.ForIteratorStmtContext ctx) {
        Value iterable = ctx.expr().accept(this);
        final ArrayType arrayType;
        final Value size = value(allocFreedRegister(), BuiltInType.INT.type());
        if (iterable.type instanceof ListType listType) {
            final Register arrayRegister = allocFreedRegister(iterable.register);
            final Symbol sizeFunction = listType.resolveMember(BuiltInFunction.LIST_SIZE.ident());
            emit(OpCode.Invoke, size.register, iterable.register, sizeFunction);
            emit(OpCode.ldFld(listType.arrayType()), arrayRegister, iterable.register, listType.arrayField().address());
            iterable = value(arrayRegister, listType.arrayType());
            arrayType = listType.arrayType();
        } else {
            if (iterable.type instanceof ArrayType == false) {
                return logLocalError(ctx, "iterable is not an array");
            }
            arrayType = (ArrayType) iterable.type;
            final Symbol sizeFunction = arrayType.resolveMember(BuiltInFunction.ARRAY_SIZE.ident());
            emit(OpCode.Invoke, size.register, iterable.register, sizeFunction);
        }
        final Value from = value(allocFreedRegister(), BuiltInType.INT.type());
        emit(OpCode.Ldc_zero, from.register);
        final Label loopLabel = addLabel();
        final Label exitLabel = addLabel();
        final Register condRegister = allocFreedRegister();
        final Register stepRegister = allocFreedRegister();
        emit(OpCode.Ldc_i32, stepRegister, 1);
        emit(OpCode.Lt_i32, condRegister, from.register, size.register);
        loopLabel.setTarget(this.currentInstructions.get(this.currentInstructions.size() - 1));
        emitBranch(condRegister, exitLabel);
        freeRegister(condRegister);
        final Value target = value(allocFreedRegister(), arrayType.elementType());
        emit(OpCode.ldElem(arrayType.elementType()), target.register, iterable.register, from.register);
        VariableSymbol iterator = emitIdentAssign(ctx.parameter(), ctx.parameter().Ident().getText(), target, true);
        assert iterator != null && iterator.isGlobal() == false;
        ctx.block().accept(this);
        emit(OpCode.Collect);
        emit(OpCode.Add_i32, from.register, from.register, stepRegister);
        emitBranch(loopLabel);
        exitLabel.setTarget(emitNop());
        freeRegister(iterable.register, from.register, size.register, target.register, stepRegister);
        return null;
    }

    @Override
    public Value visitYieldStmt(ZLangParser.YieldStmtContext ctx) {
        final BlockExprInfo top = this.blockExprInfos.peek();
        assert top != null;
        final Value value = ctx.expr().accept(this);
        if (top.resultType == null) {
            top.resultType = value.type;
        } else {
            if (Types.isImplicitlyConvertible(top.resultType, value.type) == false) {
                return logLocalError(ctx, "incompatible result types in block expr: " + value.type + " and " + top.resultType);
            }
        }
        emit(OpCode.Mov, top.targetRegister, value.register);
        freeRegister(value.register);
        return null;
    }

    @Override
    public Value visitLogStmt(ZLangParser.LogStmtContext ctx) {
        final Symbol print = currentScope().resolve(BuiltInFunction.PRINT.ident());
        final List<Register> argumentRegisters = allocRegisterRange(2);
        final StringBuilder prefix = new StringBuilder()
                .append('[').append(moduleName()).append(']').append(' ');
        if (this.currentFunction instanceof MethodSymbol method) {
            prefix.append(method.definingScope().scopeName()) .append("::");
        }
        prefix.append(this.currentFunction.name()).append(" - ");
        emit(OpCode.Ldc_str, argumentRegisters.get(0), prefix.toString()    );
        emit(OpCode.Ldc_i32, argumentRegisters.get(1), BuiltInType.STRING.type().id().number());
        emit(OpCode.Invoke, Register.R000, argumentRegisters.get(0), print);
        for (final var argument : ctx.arguments().expr()) {
            final Value value = argument.accept(this);
            emit(OpCode.Mov, argumentRegisters.get(0), value.register);
            emit(OpCode.Ldc_i32, argumentRegisters.get(1), value.type.registerType().id().number());
            emit(OpCode.Invoke, Register.R000, argumentRegisters.get(0), print);
            freeRegister(value.register);
        }
        emit(OpCode.Ldc_str, argumentRegisters.get(0), "\n");
        emit(OpCode.Ldc_i32, argumentRegisters.get(1), BuiltInType.STRING.type().id().number());
        emit(OpCode.Invoke, Register.R000, argumentRegisters.get(0), print);
        freeRegisters(argumentRegisters);
        return null;
    }

    @Override
    public Value visitAssertStmt(ZLangParser.AssertStmtContext ctx) {
        final Value value = ctx.expr().accept(this);
        if (Types.isImplicitlyConvertible(BuiltInType.BOOL.type(), value.type) == false) {
            return logLocalError(ctx, "expression is not of type boolean");
        }
        final Label skipLabel = addLabel();
        emit(OpCode.Eq_zero, value.register, value.register);
        emitBranch(value.register, skipLabel);
        freeRegister(value.register);
        final String message = "[%s] %s - assertion failed @ line %d\n".formatted(
                moduleName(), this.currentFunction.name(), ctx.getStart().getLine());
        final List<Register> args = allocRegisterRange(2);
        emit(OpCode.Ldc_str, args.get(0), message);
        emit(OpCode.Ldc_i32, args.get(1), BuiltInType.STRING.type().id().number());
        emit(OpCode.Invoke, Register.R000, args.get(0), currentScope().resolve(BuiltInFunction.PRINT.ident()));
        freeRegisters(args);
        emit(OpCode.Ldc_i32, Register.R000, Integer.MIN_VALUE);
        emit(OpCode.Halt);
        skipLabel.setTarget(emitNop());
        return null;
    }

    @Override
    public Value visitExpr(ZLangParser.ExprContext ctx) {
        if (ctx.If() != null) {
            Label elseLabel = addLabel();
            Label exitLabel = addLabel();
            final Value condition = ctx.conditionalOrExpr(1).accept(this);
            if (condition.type != BuiltInType.BOOL.type()) {
                return logLocalError(ctx, "if condition is not of type bool, but " + condition.type);
            }
            emitBranch(condition.register, elseLabel);
            final Register target = allocFreedRegister(condition.register);
            Value result = ctx.conditionalOrExpr(0).accept(this);
            emit(OpCode.Mov, target, result.register);
            freeRegister(result.register);
            emitBranch(exitLabel);
            final int alternativeIndex = this.currentInstructions.size();
            final Value alternative = ctx.expr().accept(this);
            final Type resultType = Types.promote(result.type, alternative.type);
            if (resultType == null) {
                return logLocalError(ctx, "incompatible types in ternary expression");
            }
            result = value(result.register, resultType);
            emit(OpCode.Mov, target, alternative.register);
            freeRegister(alternative.register);
            elseLabel.setTarget(this.currentInstructions.get(alternativeIndex));
            exitLabel.setTarget(emitNop());
            return value(target, result.type);
        }

        return ctx.conditionalOrExpr(0).accept(this);
    }

    @Override
    public Value visitConditionalOrExpr(ZLangParser.ConditionalOrExprContext ctx) {
        if (ctx.Or() == null) {
            return super.visitConditionalOrExpr(ctx);
        }
        final Value left = ctx.conditionalAndExpr().accept(this);
        final Value right = ctx.conditionalOrExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (left.type != BuiltInType.BOOL.type() || right.type != BuiltInType.BOOL.type()) {
            return logLocalError(ctx, "non-boolean operand in OR expression");
        }

        emit(OpCode.Or, target, left.register, right.register);
        return value(target, BuiltInType.BOOL.type());
    }

    @Override
    public Value visitConditionalAndExpr(ZLangParser.ConditionalAndExprContext ctx) {
        if (ctx.And() == null) {
            return super.visitConditionalAndExpr(ctx);
        }
        final Value left = ctx.relationalExpr().accept(this);
        final Value right = ctx.conditionalAndExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (left.type != BuiltInType.BOOL.type() || right.type != BuiltInType.BOOL.type()) {
            return logLocalError(ctx, "non-boolean operand in AND expression");
        }

        emit(OpCode.And, target, left.register, right.register);
        return value(target, BuiltInType.BOOL.type());
    }

    @Override
    public Value visitRelationalExpr(ZLangParser.RelationalExprContext ctx) {
        if (ctx.relationalOp() == null) {
            return super.visitRelationalExpr(ctx);
        }

        final Value left = ctx.typeCheckExpr(0).accept(this);
        final Value right = ctx.typeCheckExpr(1).accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (Types.isComparable(left.type, right.type) == false) {
            return logLocalError(ctx, "incompatible operand types in relational expression");
        }

        final OpCode opc;
        if (ctx.relationalOp().Eq() != null) {
            opc = OpCode.eq(left.type);
        } else if(ctx.relationalOp().Ne() != null) {
            opc = OpCode.ne(left.type);
        } else if(ctx.relationalOp().Gt() != null) {
            opc = OpCode.gt(left.type);
        } else if(ctx.relationalOp().Ge() != null) {
            opc = OpCode.ge(left.type);
        } else if(ctx.relationalOp().Lt() != null) {
            opc = OpCode.lt(left.type);
        } else if(ctx.relationalOp().Le() != null) {
            opc = OpCode.le(left.type);
        } else {
            throw new UnsupportedOperationException("unsupported relational operator");
        }

        emit(opc, target, left.register, right.register);
        return value(target, BuiltInType.BOOL.type());
    }

    @Override
    public Value visitTypeCheckExpr(ZLangParser.TypeCheckExprContext ctx) {
        if (ctx.Is() == null) {
            return ctx.additiveExpr().accept(this);
        }
        final Value left = ctx.additiveExpr().accept(this);
        if (left.type.registerType().isReferenceType() == false) {
            return logLocalError(ctx.additiveExpr(), "type checks are only valid for objects of reference types, not for " + left.type);
        }
        final Type type = resolveType(ctx.type());
        if (type == null) {
            return logLocalError(ctx.type(), "could not resolve type " + ctx.type().getText());
        }
        if (type instanceof AggregateType == false && type instanceof InterfaceSymbol == false) {
            return logLocalError(ctx.type(), "type checks are only valid for user types, not for " + type);
        }
        final Register target = allocFreedRegister(left.register);
        emit(OpCode.TypeChk, target, left.register, (Symbol) type);
        return value(target, BuiltInType.BOOL.type());
    }

    @Override
    public Value visitAdditiveExpr(ZLangParser.AdditiveExprContext ctx) {
        if (ctx.additiveOp() == null) {
            return super.visitAdditiveExpr(ctx);
        }

        final Value left = ctx.additiveExpr().accept(this);
        final Value right = ctx.multiplicativeExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        final Type targetType = Types.promote(left.type, right.type);
        if (targetType == null) {
            return logLocalError(ctx, "incompatible operand types in additive expression");
        }

        final OpCode opc;
        if (ctx.additiveOp().Plus() != null) {
            opc = OpCode.add(left.type);
        } else if(ctx.additiveOp().Minus() != null) {
            opc = OpCode.sub(left.type);
        } else if(ctx.additiveOp().Band() != null) {
            opc = OpCode.band(left.type);
        } else if(ctx.additiveOp().Bor() != null) {
            opc = OpCode.bor(left.type);
        } else if(ctx.additiveOp().Xor() != null) {
            opc = OpCode.xor(left.type);
        } else {
            throw new UnsupportedOperationException("unsupported additive operator");
        }

        emit(opc, target, left.register, right.register);
        return value(target, targetType);
    }

    @Override
    public Value visitMultiplicativeExpr(ZLangParser.MultiplicativeExprContext ctx) {
        if (ctx.multiplicativeOp() == null) {
            return super.visitMultiplicativeExpr(ctx);
        }

        final Value left = ctx.multiplicativeExpr().accept(this);
        final Value right = ctx.unaryExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        final Type targetType = Types.promote(left.type, right.type);
        if (targetType == null) {
            return logLocalError(ctx, "incompatible operand types in multiplicative expression");
        }

        final OpCode opc;
        if (ctx.multiplicativeOp().Times() != null) {
            opc = OpCode.mul(left.type);
        } else if(ctx.multiplicativeOp().Div() != null) {
            opc = OpCode.div(left.type);
        } else if(ctx.multiplicativeOp().Mod() != null) {
            opc = OpCode.mod(left.type);
        } else if(ctx.multiplicativeOp().LShift() != null) {
            opc = OpCode.leftShift(left.type);
        } else if(ctx.multiplicativeOp().RShift() != null) {
            opc = OpCode.rightShift(left.type);
        } else {
            throw new UnsupportedOperationException("unsupported multiplicative operator");
        }

        emit(opc, target, left.register, right.register);
        return value(target, targetType);
    }

    @Override
    public Value visitUnaryExpr(ZLangParser.UnaryExprContext ctx) {
        if (ctx.unaryOp() == null) {
            return super.visitUnaryExpr(ctx);
        }

        final Value value = ctx.unaryExpr().accept(this);

        if (ctx.unaryOp().Minus() != null) {
            final OpCode opcSub = OpCode.sub(
                    Types.isEffectivelyInteger(value.type)
                            ? BuiltInType.INT.type()
                            : value.type);
            final Register zero = allocFreedRegister();
            emit(OpCode.Ldc_zero, zero);
            final Register target = allocFreedRegister(value.register, zero);
            emit(opcSub, target, zero, value.register);
            return value(target, value.type);
        }

        if (ctx.unaryOp().Not() != null) {
            final Register target = allocFreedRegister(value.register);
            emit(OpCode.Eq_zero, target, value.register);
            return value(target, value.type);
        }

        throw new UnsupportedOperationException("unsupported unary expression");
    }

    @Override
    public Value visitCastExpr(ZLangParser.CastExprContext ctx) {
        final Type type = resolveType(ctx.type());
        final Value v = ctx.unaryExpr().accept(this);
        if (Types.isImplicitlyConvertible(type, v.type)) {
            return value(v.register, type);
        }
        final Register target = allocFreedRegister(v.register);
        emit(OpCode.conv(v.type), target, v.register, type.registerType().id().number());
        return value(target, type);
    }

    @Override
    public Value visitPostFixedPrimary(ZLangParser.PostFixedPrimaryContext ctx) {
        if (ctx.primary() != null) {
            return ctx.primary().accept(this);
        }

        Value primary = ctx.postFixedPrimary().accept(this);

        if (ctx.arrayAccessPostfix() != null) {
            if (primary.type instanceof ListType listType) {
                final Register arrayRegister = allocFreedRegister(primary.register);
                emit(OpCode.ldFld(listType.arrayType()), arrayRegister, primary.register, listType.arrayField().address());
                primary = value(arrayRegister, listType.arrayType());
            }
            if (primary.type instanceof ArrayType == false) {
                return logLocalError(ctx, "indexed value is not an array");
            }
            final Type elementType = ((ArrayType) primary.type).elementType();
            final Value index = ctx.arrayAccessPostfix().expr().accept(this);
            if (Types.isEffectivelyInteger(index.type) == false) {
                return logLocalError(ctx, "index is not of type int");
            }
            final Register target = allocFreedRegister(primary.register, index.register);
            emit(OpCode.ldElem(elementType), target, primary.register, index.register);
            return value(target, elementType);
        }

        if (ctx.fieldAccessPostfix() != null) {
            if (primary.type instanceof StructSymbol == false) {
                return logLocalError(ctx, "field target is not a struct");
            }
            final MemberScope memberScope = (MemberScope) primary.type;
            if (Scopes.enclosingModule(memberScope) != Scopes.enclosingModule(currentScope())) {
                return logLocalError(ctx, "access to field of foreign struct not allowed");
            }
            final Symbol field = memberScope.resolveMember(ctx.fieldAccessPostfix().Ident().getText());
            if (field instanceof FieldSymbol == false) {
                return logLocalError(ctx, "field id does not refer to a field");
            }
            final Register target = allocFreedRegister(primary.register);
            emit(OpCode.ldFld(field.type()), target, primary.register, field.address());
            return value(target, field.type());
        }

        if (ctx.methodInvocationPostfix() != null) {
            final Value value = emitMethodInvocation(primary, ctx.methodInvocationPostfix());
            freeRegister(primary.register);
            return value;
        }

        throw new UnsupportedOperationException("unsupported postFixedPrimary");
    }

    private Value emitMethodInvocation(Value primary, ZLangParser.MethodInvocationPostfixContext ctx) {
        if (primary.type instanceof MemberScope == false) {
            return logLocalError(ctx, "method invocation target is not an aggregate");
        }
        final MemberScope memberScope = (MemberScope) primary.type;
        final Symbol symbol = memberScope.resolveMember(ctx.Ident().getText());
        if (symbol instanceof MethodSymbol == false) {
            return logLocalError(ctx, "symbol is not a method: " + ctx.Ident().getText());
        }
        return emitFunctionCall(ctx, ctx.arguments(), (MethodSymbol) symbol, primary);
    }

    @Override
    public Value visitUserFunctionInvocation(ZLangParser.UserFunctionInvocationContext ctx) {
        final Symbol symbol = currentScope().resolve(ctx.Ident().getText());
        if (symbol instanceof FunctionSymbol == false) {
            return logLocalError(ctx, "undefined function '" + ctx.Ident().getText() + "'");
        }

        return emitFunctionCall(ctx, ctx.arguments(), (FunctionSymbol) symbol, null);
    }

    private Value emitFunctionCall(ParserRuleContext ctx,
                                   ZLangParser.ArgumentsContext arguments,
                                   FunctionSymbol function,
                                   Value methodTarget) {
        final Register retValRegister = function.type() != null ? allocFreedRegister() : Register.R000;
        final List<ZLangParser.ExprContext> args = arguments != null ? arguments.expr() : List.of();
        final int argCount = methodTarget != null ? args.size() + 1 : args.size(); // +1 for self
        if (function.symbols().size() != argCount) {
            return logLocalError(ctx,"function expects %d parameters, but %d arguments given".formatted(
                    function.symbols().size(), argCount));
        }

        List<Register> argRegisters = allocRegisterRange(argCount);
        int registerIndex = 0;
        if (methodTarget != null) {
            registerIndex = 1;
            emit(OpCode.Mov, argRegisters.get(0), methodTarget.register);
        }
        int argIndex = 0;
        for (final Symbol param : function.symbols()) {
            if (param instanceof VariableSymbol v && v.isSelf()) {
                continue;
            }
            final var value = args.get(argIndex).accept(this);
            if (Types.isAssignable(param.type(), value.type) == false) {
                return logLocalError(ctx, "incompatible argument type for parameter '%s'. left='%s', right='%s'"
                        .formatted(param.name(), param.type(), value.type));
            }
            emit(OpCode.Mov, argRegisters.get(registerIndex), value.register);
            freeRegister(value.register);
            argIndex++;
            registerIndex++;
        }

        emit(getCallOpCode(function), retValRegister,
                argRegisters.isEmpty() ? Register.R000 : argRegisters.get(0),
                function);
        freeRegisters(argRegisters);
        return function.type() != null
                ? value(retValRegister, function.type())
                : null;
    }

    private static OpCode getCallOpCode(FunctionSymbol callee) {
        if (callee.isBuiltIn()) {
            return OpCode.Invoke;
        }
        if (callee instanceof InterfaceMethodSymbol) {
            return OpCode.CallVirt;
        }
        return OpCode.Call;
    }

    @Override
    public Value visitPrimary(ZLangParser.PrimaryContext ctx) {
        if (ctx.Self() != null) {
            final Symbol self = currentScope().resolve(SelfSymbol.IDENT);
            if (self == null) {
                return logLocalError(ctx, "self is not defined in this context");
            }
            return value(Register.fromNumber(self.address()), self.type());
        }

        if (ctx.Ident() != null) {
            final Symbol symbol = currentScope().resolve(ctx.Ident().getText());
            if (symbol == null) {
                return logLocalError(ctx, "'" + ctx.Ident() + "' is not defined in this context");
            }
            if (symbol instanceof VariableSymbol == false) {
                return logLocalError(ctx, "'" + ctx.Ident() + "' is not a variable or constant");
            }
            final VariableSymbol variable = (VariableSymbol) symbol;
            if (variable.isGlobal()) {
                if (variable.isAssignable() && Scopes.enclosingModule(variable.definingScope()) != Scopes.enclosingModule(currentScope())) {
                    return logLocalError(ctx, "access to foreign variables not allowed");
                }
                final Register target = allocFreedRegister();
                emit(OpCode.ldGlb(symbol.type()), target, symbol.address());
                return value(target, symbol.type());
            }
            return value(Register.fromNumber(symbol.address()), symbol.type());
        }

        if (ctx.expr() != null) {
            return ctx.expr().accept(this);
        }

        return super.visitPrimary(ctx);
    }

    @Override
    public Value visitBlockExpr(ZLangParser.BlockExprContext ctx) {
        final BlockExprInfo blockExprInfo = new BlockExprInfo(allocFreedRegister());
        this.blockExprInfos.push(blockExprInfo);
        enterScope(ctx);
        super.visitBlockExpr(ctx);
        popScope();
        final BlockExprInfo poppedInfo = this.blockExprInfos.pop();
        assert poppedInfo == blockExprInfo;
        return value(blockExprInfo.targetRegister, blockExprInfo.resultType);
    }

    @Override
    public Value visitArrayInstanceCreation(ZLangParser.ArrayInstanceCreationContext ctx) {
        final Value size;
        if (ctx.expr() != null) {
            size = ctx.expr().accept(this);
        } else {
            size = value(allocFreedRegister(), BuiltInType.INT.type());
            emit(OpCode.Ldc_i32, size.register, ctx.arguments().expr().size());
        }
        final Type elementType = resolveType(ctx.type());
        final Register target = allocFreedRegister(size.register);
        emit(OpCode.newArr(elementType), target, size.register);
        final Type arrayType = defineArrayType(elementType);
        final Value array = value(target, arrayType);
        if (ctx.arguments() != null) {
            int i = 0;
            final Register indexRegister = allocFreedRegister();
            for (final var elemCtx : ctx.arguments().expr()) {
                final Value elem = elemCtx.accept(this);
                if (Types.isAssignable(elementType, elem.type) == false) {
                    return logLocalError(elemCtx, "type is not assignable to array element type");
                }
                emit(OpCode.Ldc_i32, indexRegister, i);
                emit(OpCode.stElem(elementType), elem.register, array.register, indexRegister);
                i++;
            }
            freeRegister(indexRegister);
        }
        return array;
    }

    @Override
    public Value visitListInstanceCreation(ZLangParser.ListInstanceCreationContext ctx) {
        final Type elementType = resolveType(ctx.type());
        final Register target = allocFreedRegister();
        final List<Register> registers = allocRegisterRange(2);
        final Register sizeRegister = registers.get(0);
        final Register arrayRegister = registers.get(1);
        final ListType listType = defineListType(elementType);
        emit(OpCode.NewObj, target, listType);
        emit(OpCode.Ldc_i32, sizeRegister, 0);
        emit(OpCode.stFld(elementType), sizeRegister, target, listType.sizeField().address());
        emit(OpCode.Ldc_i32, sizeRegister, 16);
        emit(OpCode.newArr(elementType), arrayRegister, sizeRegister);
        emit(OpCode.stFld(listType.arrayType()), arrayRegister, target, listType.arrayField().address());
        if (ctx.arguments() != null) {
            final Symbol addFunction = listType.resolveMember(BuiltInFunction.LIST_ADD.ident());
            emit(OpCode.Mov, registers.get(0), target);
            assert addFunction != null;
            for (final var expr : ctx.arguments().expr()) {
                final Value value = expr.accept(this);
                if (Types.isAssignable(elementType, value.type) == false) {
                    return logLocalError(expr, "incompatible types in list element assignment");
                }
                emit(OpCode.Mov, registers.get(1), value.register);
                emit(OpCode.Invoke, Register.R000, registers.get(0), addFunction);
                freeRegister(value.register);
            }
        }
        freeRegisters(registers);
        return new Value(target, listType);
    }

    @Override
    public Value visitListInstanceCreationFromArray(ZLangParser.ListInstanceCreationFromArrayContext ctx) {
        final Type elementType = resolveType(ctx.type());
        final Register target = allocFreedRegister();
        final ListType listType = defineListType(elementType);
        final Value array = ctx.expr().accept(this);
        if (array.type instanceof ArrayType == false) {
            return logLocalError(ctx, "expression is not an array type");
        }
        final ArrayType arrayType = (ArrayType) array.type;
        if (arrayType.elementType() != elementType) {
            return logLocalError(ctx, "array element type does not match list element type");
        }
        final Register sizeRegister = allocFreedRegister();
        final Symbol sizeFunction = arrayType.resolveMember(BuiltInFunction.ARRAY_SIZE.ident());
        emit(OpCode.Invoke, sizeRegister, array.register, sizeFunction);
        emit(OpCode.NewObj, target, listType);
        emit(OpCode.AddRef, target);
        emit(OpCode.stFld(listType.arrayType()), array.register, target, listType.arrayField().address());
        emit(OpCode.stFld(BuiltInType.INT.type()), sizeRegister, target, listType.sizeField().address());
        freeRegister(sizeRegister, array.register);
        return new Value(target, listType);
    }

    @Override
    public Value visitStructInstanceCreation(ZLangParser.StructInstanceCreationContext ctx) {
        final Type type = resolveType(ctx, ctx.Ident().getText());
        if (type instanceof StructSymbol == false) {
            return logLocalError(ctx, "'" + type + "' is not a struct");
        }
        if (Scopes.enclosingModule((Scope) type) != Scopes.enclosingModule(currentScope())) {
            return logLocalError(ctx, "instance creation of foreign struct not allowed");
        }
        final Symbol typeSymbol = (Symbol) type;
        final MemberScope typeScope = (MemberScope) typeSymbol;
        final Register target = allocFreedRegister();
        emit(OpCode.NewObj, target, typeSymbol);
        for (final var initializer : ctx.fieldInitializer()) {
            final Symbol field = typeScope.resolveMember(initializer.Ident().getText());
            if (field instanceof FieldSymbol == false) {
                return logLocalError(ctx, "field id does not refer to a field: " + initializer.Ident().getText());
            }
            final Value rvalue = initializer.expr().accept(this);
            if (Types.isAssignable(field.type(), rvalue.type) == false) {
                return logLocalError(ctx, "incompatible types in assignment to '%s'. left='%s', right='%s'"
                        .formatted(field.name(), field.type(), rvalue.type));
            }
            emit(OpCode.stFld(rvalue.type), rvalue.register, target, field.address());
            freeRegister(rvalue.register);
        }
        return value(target, type);
    }

    @Override
    public Value visitUnionInstanceCreation(ZLangParser.UnionInstanceCreationContext ctx) {
        final Type type = resolveType(ctx, ctx.Ident(0).getText());
        if (type instanceof UnionSymbol == false) {
            return logLocalError(ctx, "'" + type + "' is not a union");
        }
        final UnionSymbol typeSymbol = (UnionSymbol) type;
        final Register target = allocFreedRegister();
        emit(OpCode.NewObj, target, typeSymbol);
        final Symbol field = typeSymbol.resolveMember(ctx.Ident(1).getText());
        if (field instanceof FieldSymbol == false) {
            return logLocalError(ctx, "field id does not refer to a field: " + ctx.Ident(1).getText());
        }
        final int fieldId = typeSymbol.getFieldId(field);
        final Symbol flagField = typeSymbol.flagField();
        final Value rvalue = ctx.expr() != null
                ? ctx.expr().accept(this)
                : value(allocFreedRegister(), NilType.INSTANCE);
        if (Types.isAssignable(field.type(), rvalue.type) == false) {
            return logLocalError(ctx, "incompatible types in union creation to '%s'.'%s'. left='%s', right='%s'"
                    .formatted(typeSymbol.name(), field.name(), field.type(), rvalue.type));
        }
        emit(OpCode.stFld(rvalue.type), rvalue.register, target, field.address());
        final Register flagRegister = allocFreedRegister(rvalue.register);
        emit(OpCode.Ldc_i32, flagRegister, fieldId);
        emit(OpCode.stFld(flagField.type()), flagRegister, target, flagField.address());
        freeRegister(flagRegister);
        return value(target, type);
    }

    @Override
    public Value visitSwitchOverUnion(ZLangParser.SwitchOverUnionContext ctx) {
        final Value union = ctx.expr().accept(this);
        if (union.type instanceof UnionSymbol == false) {
            return logLocalError(ctx, "switched symbol is not a union, but a " + union.type);
        }

        final UnionSymbol unionType = (UnionSymbol) union.type;
        final Register flagRegister = allocFreedRegister();
        final Label exitLabel = addLabel();
        final Register resultRegister = allocFreedRegister();
        final Set<String> unvisitedFieldNames = unionType.symbols().stream()
                .filter(s -> s != unionType.flagField())
                .map(Symbol::name)
                .collect(Collectors.toSet());
        emit(OpCode.ldFld(unionType.flagField().type()), flagRegister, union.register, unionType.flagField().address());

        Type resultType = null;
        for (final var branch : ctx.switchUnionFieldClause()) {
            final Type branchType = emitSwitchUnionFieldClause(
                    branch, unionType, union.register, flagRegister, resultRegister, exitLabel);
            if (resultType == null) {
                resultType = branchType;
            } else {
                if (Types.isAssignable(resultType, branchType) == false) {
                    return logLocalError(branch.expr(), "incompatible result types in switch clauses");
                }
            }
            final String fieldIdent = branch.unionParameter().Ident().getText();
            unvisitedFieldNames.remove(fieldIdent);
        }

        if (ctx.switchUnionElseClause() != null) {
            final Value elseValue = ctx.switchUnionElseClause().expr().accept(this);
            if (Types.isAssignable(resultType, elseValue.type) == false) {
                return logLocalError(ctx.switchUnionElseClause(), "incompatible result types in switch clauses");
            }
            emit(OpCode.Mov, resultRegister, elseValue.register);
        } else {
            if (unvisitedFieldNames.isEmpty() == false) {
                return logLocalError(ctx, "missing matches for union fields " + unvisitedFieldNames);
            }
        }

        freeRegister(flagRegister, union.register);
        exitLabel.setTarget(emitNop());
        return value(resultRegister, resultType);
    }

    private Type emitSwitchUnionFieldClause(ZLangParser.SwitchUnionFieldClauseContext branch,
                                            UnionSymbol union,
                                            Register unionRegister,
                                            Register flagRegister,
                                            Register resultRegister,
                                            Label exitLabel) {
        enterScope(branch);
        final Label skipLabel = addLabel();
        final String fieldIdent = branch.unionParameter().Ident().getText();
        final Symbol field = union.resolveMember(fieldIdent);
        final Symbol fieldLocal = currentScope().resolve(fieldIdent);
        if (fieldLocal.type() != field.type()) {
            logLocalError(branch, "type mismatch for union field " + field.name());
            return null;
        }
        final Register fieldFlagRegister = allocFreedRegister();
        emit(OpCode.Ldc_i32, fieldFlagRegister, union.getFieldId(field));
        final Register cmpRegister = allocFreedRegister(fieldFlagRegister);
        emit(OpCode.eq(union.flagField().type()), cmpRegister, fieldFlagRegister, flagRegister);
        emitBranch(cmpRegister, skipLabel);
        freeRegister(cmpRegister, fieldFlagRegister);
        emit(OpCode.ldFld(field.type()), Register.fromNumber(fieldLocal.address()), unionRegister, field.address());
        final Value branchValue = branch.expr().accept(this);
        emit(OpCode.Mov, resultRegister, branchValue.register);
        emitBranch(exitLabel);
        skipLabel.setTarget(emitNop());
        popScope();
        return branchValue.type;
    }

    @Override
    public Value visitLiteral(ZLangParser.LiteralContext ctx) {
        if (ctx.True() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_i32, target, 1);
            return value(target, BuiltInType.BOOL.type());
        }
        if (ctx.False() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, BuiltInType.BOOL.type());
        }
        if (ctx.Nil() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, NilType.INSTANCE);
        }
        if (ctx.NullPtr() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, BuiltInType.RUNTIME_PTR.type());
        }
        if (ctx.StringLiteral() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_str, target, CharMatcher.is('"').trimFrom(ctx.StringLiteral().getText()));
            return value(target, BuiltInType.STRING.type());
        }
        if (ctx.CharLiteral() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_i32, target, (int) ctx.CharLiteral().getText().charAt(1));
            return value(target, BuiltInType.BYTE.type());
        }
        if (ctx.number() != null) {
            return ctx.number().accept(this);
        }
        throw new UnsupportedOperationException("unsupported literal");
    }

    @Override
    public Value visitNumber(ZLangParser.NumberContext ctx) {
        final Register target = allocFreedRegister();
        final boolean negative = ctx.Minus() != null;
        if (ctx.IntegerNumber() != null) {
            int literal = parseInteger(ctx.IntegerNumber().getText());
            if (negative) {
                literal = -literal;
            }
            emit(OpCode.Ldc_i32, target, literal);
            return value(target, getIntegerLiteralType(literal));
        }
        if (ctx.RealNumber() != null) {
            double literal = parseFloat(ctx.RealNumber().getText());
            if (negative) {
                literal = -literal;
            }
            emit(OpCode.Ldc_f64, target, literal);
            return value(target, BuiltInType.FLOAT.type());
        }
        if (ctx.HexNumber() != null) {
            int literal = parseHexNumber(ctx.HexNumber().getText());
            if (negative) {
                literal = -literal;
            }
            emit(OpCode.Ldc_i32, target, literal);
            return value(target, getIntegerLiteralType(literal));
        }
        throw new UnsupportedOperationException("unsupported number type");
    }

    private static Type getIntegerLiteralType(int literal) {
        if (literal < 256) {
            return BuiltInType.BYTE.type();
        }
        return BuiltInType.INT.type();
    }

    private void freeRegister(Register... registersToFree) {
        freeRegisters(Arrays.asList(registersToFree));
    }

    private void freeRegisters(List<Register> registersToFree) {
        for (final Register register : registersToFree) {
            this.allocatedRegisters.remove(register);
        }
        log.info("allocated registers: {}", this.allocatedRegisters);
    }

    private Register allocFreedRegister(Register... registersToFree) {
        freeRegister(registersToFree);
        Register r = this.firstVolatileRegister;
        while (this.allocatedRegisters.contains(r)) {
            r = r.next();
        }
        this.allocatedRegisters.add(r);
        log.info("allocated registers: {}", this.allocatedRegisters);
        return r;
    }

    private List<Register> allocRegisterRange(int count) {
        final List<Register> registers = new ArrayList<>();
        Register r = getHighestAllocatedRegister().next();
        for (int i = 0; i < count; i++) {
            this.allocatedRegisters.add(r);
            registers.add(r);
            r = r.next();
        }
        log.info("allocated registers: {}", this.allocatedRegisters);
        return registers;
    }

    private Register getHighestAllocatedRegister() {
        Register highest = this.firstVolatileRegister;
        for (final Register r : this.allocatedRegisters) {
            if (r.number() > highest.number()) {
                highest = r;
            }
        }
        return highest;
    }
    private static Value value(Register register, Type type) {
        return new Value(register, type);
    }

    private static Value emptyValue() {
        return new Value(null, null);
    }

    private static int parseInteger(String text) {
        return Integer.parseInt(CharMatcher.is('_').removeFrom(text));
    }

    private static double parseFloat(String text) {
        return Double.parseDouble(CharMatcher.is('_').removeFrom(text));
    }

    private static int parseHexNumber(String text) {
        return Integer.parseInt(CharMatcher.is('_').removeFrom(text.substring(2)), 16);
    }

    private void enterFunction(FunctionSymbol function, List<Instruction> instructions) {
        log.info("entering function: {}", function.name());
        this.currentInstructions = instructions;
        this.codeMap.computeIfAbsent(function, ignored -> {
            final Instruction nop = emitNop();
            function.symbols().stream()
                    .filter(s -> s instanceof VariableSymbol && s.type().registerType().isReferenceType())
                    .forEach(s -> emit(OpCode.AddRef, Register.fromNumber(s.address())));
            return nop;
        });
        this.currentFunction = function;
        int top = this.currentFunction.symbols().size() + this.currentFunction.localCount() + 1;
        this.firstVolatileRegister = Register.fromNumber(top);
        this.allocatedRegisters.clear();
    }

    private void emitFunctionCleanup(FunctionSymbol function) {
        Stream.concat(function.symbols().stream(), function.locals().stream())
                .filter(s -> s instanceof VariableSymbol && s.type().registerType().isReferenceType())
                .forEach(s -> emit(OpCode.RemoveRef, Register.fromNumber(s.address())));
        emit(OpCode.Collect);
    }

    private Instruction emitNop() {
        final Instruction instr = new Instruction(OpCode.Nop);
        this.currentInstructions.add(instr);
        return instr;
    }

    private void emit(OpCode opCode) {
        this.currentInstructions.add(new Instruction(opCode));
    }

    private void emit(OpCode opCode, Register register) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register1, Register register2) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register1);
        instr.setRegisterArg(1, register2);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register1, Register register2, Register register3) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register1);
        instr.setRegisterArg(1, register2);
        instr.setRegisterArg(2, register3);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register1, Register register2, long integer) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register1);
        instr.setRegisterArg(1, register2);
        instr.setIntArg(integer);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, long integer) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setIntArg(integer);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, double floatArg) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setFloatArg(floatArg);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, String strArg) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setStrArg(strArg);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, Symbol symbol) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setSymbolArg(symbol);
        this.currentInstructions.add(instr);
    }

    private void emit(OpCode opCode, Register register1, Register register2, Symbol symbol) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register1);
        instr.setRegisterArg(1, register2);
        instr.setSymbolArg(symbol);
        this.currentInstructions.add(instr);
    }

    private void emitBranch(Register register, Label label) {
        final Instruction instr = new Instruction(OpCode.Br_zero);
        instr.setRegisterArg(0, register);
        instr.setLabelArg(label);
        label.addSource(instr);
        this.currentInstructions.add(instr);
    }

    private void emitBranch(Label label) {
        final Instruction instr = new Instruction(OpCode.Br);
        instr.setLabelArg(label);
        label.addSource(instr);
        this.currentInstructions.add(instr);
    }

    private Label addLabel() {
        final Label label = new Label();
        this.labels.add(label);
        return label;
    }

    static record Value(Register register, Type type) { }

    private Value logLocalError(ParserRuleContext ctx, String message) {
        logSemanticError(ctx, message);
        return emptyValue();
    }

    private static class BlockExprInfo {
        final Register targetRegister;
        Type resultType;

        BlockExprInfo(Register targetRegister) {
            this.targetRegister = targetRegister;
        }
    }
}
