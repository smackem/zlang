package net.smackem.zlang.emit.ir;

import com.google.common.base.CharMatcher;
import net.smackem.zlang.lang.ZLangParser;
import net.smackem.zlang.symbols.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class EmitWalker extends ScopeWalker<EmitWalker.Value> {
    private final static Logger log = LoggerFactory.getLogger(EmitWalker.class);
    private final List<Type> types = new ArrayList<>();
    private final List<FunctionSymbol> functions = new ArrayList<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private final Map<FunctionSymbol, Instruction> codeMap = new HashMap<>();
    private final FunctionSymbol initFunction;
    private final Set<Register> allocatedRegisters = new HashSet<>();
    private final List<Instruction> initInstructions = new ArrayList<>();
    private List<Instruction> currentInstructions = instructions;
    private FunctionSymbol currentFunction;
    private Register firstVolatileRegister;

    EmitWalker(String moduleName, ProgramStructure programStructure) {
        super(programStructure.globalScope(), programStructure.scopes());
        this.initFunction = new FunctionSymbol("@init:" + moduleName, null, programStructure.globalScope());
        this.functions.add(this.initFunction);
    }

    public EmittedModule buildModule() {
        if (this.initInstructions.isEmpty() == false) {
            this.initInstructions.add(new Instruction(OpCode.Ret));
        }
        this.instructions.addAll(this.initInstructions);
        return new EmittedModule(this.types, this.functions, this.instructions, this.codeMap);
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
        emit(function.isEntryPoint() ? OpCode.Halt : OpCode.Ret);
        popScope();
        this.currentFunction = null;
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
            final Value rvalue = expr.accept(this);
            emitIdentAssign(ctx, ident, rvalue, true);
        }
    }

    @Override
    public Value visitAssignStmt(ZLangParser.AssignStmtContext ctx) {
        final Value rvalue = ctx.expr().accept(this);
        if (ctx.postFixedPrimary() != null) {
            emitPostFixedPrimaryAssign(ctx.postFixedPrimary(), rvalue);
        } else {
            emitIdentAssign(ctx, ctx.Ident().getText(), rvalue, false);
        }
        return null;
    }

    private void emitIdentAssign(ParserRuleContext ctx, String ident, Value rvalue, boolean init) {
        final Symbol symbol = currentScope().resolve(ident);
        if (symbol == null) {
            logLocalError(ctx, "'" + ident + "' is not defined in this context");
            return;
        }
        if (symbol instanceof VariableSymbol == false) {
            logLocalError(ctx, "'" + ident + "' is not a variable");
            return;
        }
        final VariableSymbol variable = (VariableSymbol) symbol;
        if (init == false && variable.isAssignable() == false) {
            logLocalError(ctx, "'" + ident + "' is not assignable");
            return;
        }
        if (Types.isAssignable(variable.type(), rvalue.type) == false) {
            logLocalError(ctx, "incompatible types in assignment");
            return;
        }
        if (variable.isGlobal()) {
            emit(OpCode.stGlb(symbol.type()), rvalue.register, symbol.address());
        } else {
            emit(OpCode.Mov, Register.fromNumber(symbol.address()), rvalue.register);
        }
        freeRegister(rvalue.register);
    }

    public void emitPostFixedPrimaryAssign(ZLangParser.PostFixedPrimaryContext ctx, Value rvalue) {
        final Value primary = ctx.primary() != null
                ? ctx.primary().accept(this)
                : ctx.postFixedPrimary().accept(this);

        if (ctx.arrayAccessPostfix() != null) {
            if (primary.type instanceof ArrayType == false) {
                logLocalError(ctx, "indexed value is not an array");
                return;
            }
            final Value index = ctx.arrayAccessPostfix().expr().accept(this);
            if (index.type != BuiltInTypeSymbol.INT) {
                logLocalError(ctx, "index is not of type int");
                return;
            }
            final ArrayType arrayType = (ArrayType) primary.type;
            if (Types.isAssignable(arrayType.elementType(), rvalue.type) == false) {
                logLocalError(ctx, "incompatible types in assignment");
                return;
            }
            emit(OpCode.stElem(arrayType.elementType()), rvalue.register, primary.register, index.register);
            freeRegister(rvalue.register, primary.register, index.register);
            return;
        }

        if (ctx.fieldAccessPostfix() != null) {
            if (primary.type instanceof AggregateType == false) {
                logLocalError(ctx, "field target is not an aggregate");
                return;
            }
            final MemberScope memberScope = (MemberScope) primary.type;
            final Symbol field = memberScope.resolveMember(ctx.fieldAccessPostfix().Ident().getText());
            if (field instanceof FieldSymbol == false) {
                logLocalError(ctx, "field id does not refer to a field");
                return;
            }
            if (Types.isAssignable(field.type(), rvalue.type) == false) {
                logLocalError(ctx, "incompatible types in assignment");
                return;
            }
            emit(OpCode.stFld(field.type()), rvalue.register, primary.register, field.address());
            freeRegister(rvalue.register, primary.register);
            return;
        }

        throw new UnsupportedOperationException("unsupported postFixedPrimary");
    }

    @Override
    public Value visitReturnStmt(ZLangParser.ReturnStmtContext ctx) {
        final Value value = ctx.expr().accept(this);
        if (Types.isAssignable(this.currentFunction.type(), value.type()) == false) {
            return logLocalError(ctx, "incompatible return type");
        }
        emit(OpCode.Mov, Register.R000, value.register);
        freeRegister(value.register);
        return super.visitReturnStmt(ctx);
    }

    @Override
    public Value visitConditionalOrExpr(ZLangParser.ConditionalOrExprContext ctx) {
        if (ctx.Or() == null) {
            return super.visitConditionalOrExpr(ctx);
        }
        final Value left = ctx.conditionalAndExpr().accept(this);
        final Value right = ctx.conditionalOrExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (left.type != BuiltInTypeSymbol.BOOL || right.type != BuiltInTypeSymbol.BOOL) {
            return logLocalError(ctx, "non-boolean operand in OR expression");
        }

        emit(OpCode.Or, target, left.register, right.register);
        return value(target, BuiltInTypeSymbol.BOOL);
    }

    @Override
    public Value visitConditionalAndExpr(ZLangParser.ConditionalAndExprContext ctx) {
        if (ctx.And() == null) {
            return super.visitConditionalAndExpr(ctx);
        }
        final Value left = ctx.relationalExpr().accept(this);
        final Value right = ctx.conditionalAndExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (left.type != BuiltInTypeSymbol.BOOL || right.type != BuiltInTypeSymbol.BOOL) {
            return logLocalError(ctx, "non-boolean operand in AND expression");
        }

        emit(OpCode.And, target, left.register, right.register);
        return value(target, BuiltInTypeSymbol.BOOL);
    }

    @Override
    public Value visitRelationalExpr(ZLangParser.RelationalExprContext ctx) {
        if (ctx.relationalOp() == null) {
            return super.visitRelationalExpr(ctx);
        }

        final Value left = ctx.additiveExpr(0).accept(this);
        final Value right = ctx.additiveExpr(1).accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (left.type != right.type) {
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

        if (opc == null) {
            return logLocalError(ctx, "unsupported type for relational operator " + left.type);
        }
        emit(opc, target, left.register, right.register);
        return value(target, left.type);
    }

    @Override
    public Value visitAdditiveExpr(ZLangParser.AdditiveExprContext ctx) {
        if (ctx.additiveOp() == null) {
            return super.visitAdditiveExpr(ctx);
        }

        final Value left = ctx.additiveExpr().accept(this);
        final Value right = ctx.multiplicativeExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (left.type != right.type) {
            return logLocalError(ctx, "incompatible operand types in additive expression");
        }

        final OpCode opc;
        if (ctx.additiveOp().Plus() != null) {
            opc = OpCode.add(left.type);
        } else if(ctx.additiveOp().Minus() != null) {
            opc = OpCode.sub(left.type);
        } else {
            throw new UnsupportedOperationException("unsupported additive operator");
        }

        if (opc == null) {
            return logLocalError(ctx, "unsupported type for additive operator " + left.type);
        }
        emit(opc, target, left.register, right.register);
        return value(target, left.type);
    }

    @Override
    public Value visitMultiplicativeExpr(ZLangParser.MultiplicativeExprContext ctx) {
        if (ctx.multiplicativeOp() == null) {
            return super.visitMultiplicativeExpr(ctx);
        }

        final Value left = ctx.multiplicativeExpr().accept(this);
        final Value right = ctx.unaryExpr().accept(this);
        final Register target = allocFreedRegister(left.register, right.register);
        if (left.type != right.type) {
            return logLocalError(ctx, "incompatible operand types in multiplicative expression");
        }

        final OpCode opc;
        if (ctx.multiplicativeOp().Times() != null) {
            opc = OpCode.mul(left.type);
        } else if(ctx.multiplicativeOp().Div() != null) {
            opc = OpCode.div(left.type);
        } else {
            throw new UnsupportedOperationException("unsupported multiplicative operator");
        }

        if (opc == null) {
            return logLocalError(ctx, "unsupported type for multiplicative operator " + left.type);
        }
        emit(opc, target, left.register, right.register);
        return value(target, left.type);
    }

    @Override
    public Value visitUnaryExpr(ZLangParser.UnaryExprContext ctx) {
        if (ctx.unaryOp() == null) {
            return super.visitUnaryExpr(ctx);
        }

        final Value value = ctx.unaryExpr().accept(this);

        if (ctx.unaryOp().Minus() != null) {
            final OpCode opcSub = OpCode.sub(value.type);
            if (opcSub == null) {
                return logLocalError(ctx, "operator - not supported for type " + value.type);
            }
            final Register zero = allocFreedRegister();
            emit(OpCode.Ldc_zero, zero);
            final Register target = allocFreedRegister(value.register, zero);
            emit(opcSub, target, zero, value.register);
            return value(target, value.type);
        }

        if (ctx.unaryOp().Not() != null) {
            final OpCode opcEq = OpCode.eq(value.type);
            if (opcEq == null) {
                return logLocalError(ctx, "operator == not supported for type " + value.type);
            }
            final Register zero = allocFreedRegister();
            emit(OpCode.Ldc_zero, zero);
            final Register target = allocFreedRegister(value.register, zero);
            emit(opcEq, target, zero, value.register);
            return value(target, value.type);
        }

        throw new UnsupportedOperationException("unsupported unary expression");
    }

    @Override
    public Value visitCastExpr(ZLangParser.CastExprContext ctx) {
        final Type type = resolveType(ctx.type());
        final Value value = ctx.unaryExpr().accept(this);
        final Register target = allocFreedRegister(value.register);
        emit(OpCode.conv(value.type), target, value.register, type.primitive().id());
        return value(target, type);
    }

    @Override
    public Value visitPostFixedPrimary(ZLangParser.PostFixedPrimaryContext ctx) {
        if (ctx.primary() != null) {
            return ctx.primary().accept(this);
        }

        final Value primary = ctx.postFixedPrimary().accept(this);

        if (ctx.arrayAccessPostfix() != null) {
            if (primary.type instanceof ArrayType == false) {
                return logLocalError(ctx, "indexed value is not an array");
            }
            final Type elementType = ((ArrayType) primary.type).elementType();
            final Value index = ctx.arrayAccessPostfix().expr().accept(this);
            if (index.type != BuiltInTypeSymbol.INT) {
                return logLocalError(ctx, "index is not of type int");
            }
            final Register target = allocFreedRegister(primary.register, index.register);
            emit(OpCode.ldElem(elementType), target, primary.register, index.register);
            return value(target, elementType);
        }

        if (ctx.fieldAccessPostfix() != null) {
            if (primary.type instanceof AggregateType == false) {
                return logLocalError(ctx, "field target is not an aggregate");
            }
            final MemberScope memberScope = (MemberScope) primary.type;
            final Symbol field = memberScope.resolveMember(ctx.fieldAccessPostfix().Ident().getText());
            if (field instanceof FieldSymbol == false) {
                return logLocalError(ctx, "field id does not refer to a field");
            }
            final Register target = allocFreedRegister(primary.register);
            emit(OpCode.ldFld(primary.type), target, primary.register, field.address());
            return value(target, field.type());
        }

        throw new UnsupportedOperationException("unsupported postFixedPrimary");
    }

    @Override
    public Value visitPrimary(ZLangParser.PrimaryContext ctx) {
        if (ctx.Self() != null) {
            final Symbol self = currentScope().resolve("self");
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
            if (((VariableSymbol) symbol).isGlobal()) {
                final Register target = allocFreedRegister();
                emit(OpCode.ldGlb(symbol.type()), target, symbol.address());
                return value(target, symbol.type());
            }
            return value(Register.fromNumber(symbol.address()), symbol.type());
        }

        return super.visitPrimary(ctx);
    }

    @Override
    public Value visitLiteral(ZLangParser.LiteralContext ctx) {
        if (ctx.True() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_i32, target, 1);
            return value(target, BuiltInTypeSymbol.BOOL);
        }
        if (ctx.False() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, BuiltInTypeSymbol.BOOL);
        }
        if (ctx.Nil() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, NilType.INSTANCE);
        }
        if (ctx.NullPtr() != null) {
            final Register target = allocFreedRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, BuiltInTypeSymbol.RUNTIME_PTR);
        }
        if (ctx.number() != null) {
            return ctx.number().accept(this);
        }
        throw new UnsupportedOperationException("unsupported literal");
    }

    @Override
    public Value visitNumber(ZLangParser.NumberContext ctx) {
        final Register target = allocFreedRegister();
        if (ctx.IntegerNumber() != null) {
            emit(OpCode.Ldc_i32, target, parseInteger(ctx.IntegerNumber().getText()));
            return value(target, BuiltInTypeSymbol.INT);
        }
        if (ctx.RealNumber() != null) {
            emit(OpCode.Ldc_f64, target, parseFloat(ctx.RealNumber().getText()));
            return value(target, BuiltInTypeSymbol.FLOAT);
        }
        throw new UnsupportedOperationException("unsupported number type");
    }

    private void freeRegister(Register... registersToFree) {
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

    private void enterFunction(FunctionSymbol function, List<Instruction> instructions) {
        this.currentInstructions = instructions;
        this.codeMap.computeIfAbsent(function, ignored -> {
            final Instruction nop = new Instruction(OpCode.Nop);
            this.currentInstructions.add(nop);
            return nop;
        });
        this.currentFunction = function;
        int top = this.currentFunction.symbols().size() + this.currentFunction.localCount() + 1;
        this.firstVolatileRegister = Register.fromNumber(top);
        this.allocatedRegisters.clear();
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

    public record Value(Register register, Type type) {
    }

    private Value logLocalError(ParserRuleContext ctx, String message) {
        logSemanticError(ctx, message);
        return emptyValue();
    }
}
