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
    public Value visitPostFixedPrimary(ZLangParser.PostFixedPrimaryContext ctx) {
        if (ctx.primary() != null) {
            return ctx.primary().accept(this);
        }
        final Value primary = ctx.postFixedPrimary().accept(this);
        if (ctx.arrayAccessPostfix() != null) {
            if (primary.type instanceof ArrayType == false) {
                logSemanticError(ctx, "indexed value is not an array");
                return emptyValue();
            }
            final Value index = ctx.arrayAccessPostfix().expr().accept(this);
            if (index.type != BuiltInTypeSymbol.INT) {
                logSemanticError(ctx, "index is not of type int");
                return emptyValue();
            }
        }
        return emptyValue();
    }

    @Override
    public Value visitPrimary(ZLangParser.PrimaryContext ctx) {
        if (ctx.Self() != null) {
            final Symbol self = currentScope().resolve("self");
            if (self == null) {
                logSemanticError(ctx, "self is not defined in this context");
                return emptyValue();
            }
            return value(Register.fromNumber(self.address()), self.type());
        }
        if (ctx.Ident() != null) {
            final Symbol symbol = currentScope().resolve(ctx.Ident().getText());
            if (symbol == null) {
                logSemanticError(ctx, "'" + ctx.Ident() + "' is not defined in this context");
                return emptyValue();
            }
            return value(Register.fromNumber(symbol.address()), symbol.type());
        }

        return super.visitPrimary(ctx);
    }

    @Override
    public Value visitLiteral(ZLangParser.LiteralContext ctx) {
        if (ctx.True() != null) {
            final Register target = allocRegister();
            emit(OpCode.Ldc_i32, target, 1);
            return value(target, BuiltInTypeSymbol.BOOL);
        }
        if (ctx.False() != null) {
            final Register target = allocRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, BuiltInTypeSymbol.BOOL);
        }
        if (ctx.Nil() != null) {
            final Register target = allocRegister();
            emit(OpCode.Ldc_zero, target);
            return value(target, BuiltInTypeSymbol.OBJECT);
        }
        if (ctx.NullPtr() != null) {
            final Register target = allocRegister();
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
        final Register target = allocRegister();
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

    private Register allocRegister() {
        Register r = this.firstVolatileRegister;
        while (this.allocatedRegisters.contains(r)) {
            r = r.next();
        }
        return r;
    }

    private void freeRegister(Register register) {
        this.allocatedRegisters.remove(register);
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

    public static class Value {
        private final Register register;
        private final Type type;

        private Value(Register register, Type type) {
            this.register = register;
            this.type = type;
        }
    }
}
