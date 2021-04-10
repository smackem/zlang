package net.smackem.zlang.emit.ir;

import net.smackem.zlang.lang.ZLangParser;
import net.smackem.zlang.symbols.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public class Emitter extends ScopeWalker<Type> {
    private final List<Type> types = new ArrayList<>();
    private final List<FunctionSymbol> functions = new ArrayList<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private final FunctionSymbol initFunction;
    private FunctionSymbol currentFunction;
    private Register topRegister;

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
    public Type visitVarDeclStmt(ZLangParser.VarDeclStmtContext ctx) {
        if (this.currentFunction == null) {
            // global var
            enterFunction(this.initFunction);
        } else {
            final Symbol variable = this.currentScope().resolve(ctx.parameter().Ident().getText());
        }
        return super.visitVarDeclStmt(ctx);
    }

    @Override
    public Type visitBindingStmt(ZLangParser.BindingStmtContext ctx) {
        if (this.currentFunction == null) {
            enterFunction(this.initFunction);
        }
        return super.visitBindingStmt(ctx);
    }

    @Override
    public Type visitModule(ZLangParser.ModuleContext ctx) {
        enterScope(ctx);
        super.visitModule(ctx);
        popScope();
        return null;
    }

    @Override
    public Type visitInterfaceDecl(ZLangParser.InterfaceDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitInterfaceDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Type visitStructDecl(ZLangParser.StructDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitStructDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Type visitUnionDecl(ZLangParser.UnionDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitUnionDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Type visitFunctionDecl(ZLangParser.FunctionDeclContext ctx) {
        enterScope(ctx);
        enterFunction((FunctionSymbol) currentScope());
        this.functions.add(this.currentFunction);
        super.visitFunctionDecl(ctx);
        popScope();
        this.currentFunction = null;
        return null;
    }

    @Override
    public Type visitForStmt(ZLangParser.ForStmtContext ctx) {
        enterScope(ctx);
        super.visitForStmt(ctx);
        popScope();
        return null;
    }

    @Override
    public Type visitBlock(ZLangParser.BlockContext ctx) {
        enterScope(ctx);
        super.visitBlock(ctx);
        popScope();
        return null;
    }

    @Override
    public Type visitNumber(ZLangParser.NumberContext ctx) {
        if (ctx.IntegerNumber() != null) {
            emit(OpCode.Ldc_i32, pushRegister(), parseInteger(ctx.IntegerNumber().getText()));
            return BuiltInTypeSymbol.INT;
        }
        if (ctx.RealNumber() != null) {
            emit(OpCode.Ldc_f64, pushRegister(), parseFloat(ctx.RealNumber().getText()));
            return BuiltInTypeSymbol.FLOAT;
        }
        return super.visitNumber(ctx);
    }

    private Register pushRegister() {
        final Register register = this.topRegister;
        this.topRegister = this.topRegister.next();
        return register;
    }

    private Register popRegister() {
        final Register register = this.topRegister;
        this.topRegister = this.topRegister.previous();
        return register;
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
        this.topRegister = Register.fromNumber(top);
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

    private void emit(OpCode opCode, Register register, Symbol symbol) {
        final Instruction instr = new Instruction(opCode);
        instr.setRegisterArg(0, register);
        instr.setSymbolArg(symbol);
        this.instructions.add(instr);
    }
}
