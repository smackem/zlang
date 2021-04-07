package net.smackem.zlang.emit.ir;

import net.smackem.zlang.lang.ZLangParser;
import net.smackem.zlang.symbols.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public class Emitter extends ScopeWalker {
    private final List<Type> types = new ArrayList<>();
    private final List<EmittedFunction> functions = new ArrayList<>();
    private EmittedFunction currentFunction;
    private final EmittedFunction initFunction;

    Emitter(String moduleName, GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        super(globalScope, scopes);
        this.initFunction = new EmittedFunction(new FunctionSymbol("@init:" + moduleName, null, globalScope));
        this.functions.add(this.initFunction);
    }

    Collection<Type> types() {
        return Collections.unmodifiableCollection(this.types);
    }

    Collection<EmittedFunction> functions() {
        return Collections.unmodifiableCollection(this.functions);
    }

    @Override
    public Void visitVarDeclStmt(ZLangParser.VarDeclStmtContext ctx) {
        if (this.currentFunction == null) {
            this.currentFunction = this.initFunction;
        }
        return super.visitVarDeclStmt(ctx);
    }

    @Override
    public Void visitBindingStmt(ZLangParser.BindingStmtContext ctx) {
        if (this.currentFunction == null) {
            this.currentFunction = this.initFunction;
        }
        return super.visitBindingStmt(ctx);
    }

    @Override
    public Void visitModule(ZLangParser.ModuleContext ctx) {
        enterScope(ctx);
        super.visitModule(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitInterfaceDecl(ZLangParser.InterfaceDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitInterfaceDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitStructDecl(ZLangParser.StructDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitStructDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitUnionDecl(ZLangParser.UnionDeclContext ctx) {
        enterScope(ctx);
        this.types.add((Type) currentScope());
        super.visitUnionDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitFunctionDecl(ZLangParser.FunctionDeclContext ctx) {
        enterScope(ctx);
        this.currentFunction = new EmittedFunction((FunctionSymbol) currentScope());
        this.functions.add(this.currentFunction);
        super.visitFunctionDecl(ctx);
        popScope();
        this.currentFunction = null;
        return null;
    }

    @Override
    public Void visitForStmt(ZLangParser.ForStmtContext ctx) {
        enterScope(ctx);
        super.visitForStmt(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitBlock(ZLangParser.BlockContext ctx) {
        enterScope(ctx);
        super.visitBlock(ctx);
        popScope();
        return null;
    }
}
