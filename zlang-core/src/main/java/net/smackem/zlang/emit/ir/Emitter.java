package net.smackem.zlang.emit.ir;

import net.smackem.zlang.lang.ZLangParser;
import net.smackem.zlang.symbols.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public class Emitter extends ScopeWalker<Emitter.Value> {
    private final List<Type> types = new ArrayList<>();
    private final List<FunctionSymbol> functions = new ArrayList<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private final FunctionSymbol initFunction;
    private final Set<Register> allocatedRegisters = new HashSet<>();
    private FunctionSymbol currentFunction;
    private Register firstVolatileRegister;

    Emitter(String moduleName, GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        super(globalScope, scopes);
        this.initFunction = new FunctionSymbol("@init:" + moduleName, null, globalScope);
        this.functions.add(this.initFunction);
    }

    Collection<Type> types() {
        return Collections.unmodifiableCollection(this.types);
    }

    Collection<FunctionSymbol> functions() {
        return Collections.unmodifiableCollection(this.functions);
    }

    @Override
    public Value visitVarDeclStmt(ZLangParser.VarDeclStmtContext ctx) {
        if (this.currentFunction == null) {
            // global var
            enterFunction(this.initFunction);
        }
        return super.visitVarDeclStmt(ctx);
    }

    @Override
    public Value visitBindingStmt(ZLangParser.BindingStmtContext ctx) {
        if (this.currentFunction == null) {
            // global var
            enterFunction(this.initFunction);
        }
        return super.visitBindingStmt(ctx);
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
        enterFunction((FunctionSymbol) currentScope());
        this.functions.add(this.currentFunction);
        super.visitFunctionDecl(ctx);
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
            final Value index = ctx.arrayAccessPostfix().expr().accept(this);
            if (index.type != BuiltInTypeSymbol.INT) {
                return logLocalError(ctx, "index is not of type int");
            }
            final Register target = allocFreedRegister(primary.register, index.register);
            emit(OpCode.ldElem(primary.type), target, primary.register, index.register);
            return value(target, primary.type);
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
            return value(target, BuiltInTypeSymbol.OBJECT);
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

    private Register allocFreedRegister(Register... registersToFree) {
        for (final Register register : registersToFree) {
            this.allocatedRegisters.remove(register);
        }
        Register r = this.firstVolatileRegister;
        while (this.allocatedRegisters.contains(r)) {
            r = r.next();
        }
        return r;
    }

    private static Value value(Register register, Type type) {
        return new Value(register, type);
    }

    private static Value emptyValue() {
        return new Value(null, null);
    }

    private static int parseInteger(String text) {
        return Integer.parseInt(text);
    }

    private static double parseFloat(String text) {
        return Double.parseDouble(text);
    }

    private void enterFunction(FunctionSymbol function) {
        this.currentFunction = function;
        int top = this.currentFunction.symbols().size() + this.currentFunction.localCount() + 1;
        this.firstVolatileRegister = Register.fromNumber(top);
        this.allocatedRegisters.clear();
    }

    private void emit(OpCode opCode, Register register) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        this.instructions.add(instr);
    }

    private void emit(OpCode opCode, Register register1, Register register2) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register1);
        instr.setRegisterArg(1, register2);
        this.instructions.add(instr);
    }

    private void emit(OpCode opCode, Register register1, Register register2, Register register3) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register1);
        instr.setRegisterArg(1, register2);
        instr.setRegisterArg(2, register3);
        this.instructions.add(instr);
    }

    private void emit(OpCode opCode, Register register1, Register register2, long integer) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register1);
        instr.setRegisterArg(1, register2);
        instr.setIntArg(integer);
        this.instructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, long integer) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setIntArg(integer);
        this.instructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, double floatArg) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setFloatArg(floatArg);
        this.instructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, String strArg) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setStrArg(strArg);
        this.instructions.add(instr);
    }

    private void emit(OpCode opCode, Register register, Symbol symbol) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setSymbolArg(symbol);
        this.instructions.add(instr);
    }

    public record Value(Register register, Type type) {
    }

    private Value logLocalError(ParserRuleContext ctx, String message) {
        logSemanticError(ctx, message);
        return emptyValue();
    }
}
