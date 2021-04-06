package net.smackem.zlang.emit.ir;

import net.smackem.zlang.lang.ZLangParser;
import net.smackem.zlang.symbols.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

public class Emitter extends ScopeWalker {
    private final List<Type> types = new ArrayList<>();
    private final List<FunctionSymbol> functions = new ArrayList<>();

    Emitter(GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        super(globalScope, scopes);
    }

    Collection<Type> types() {
        return Collections.unmodifiableCollection(this.types);
    }

    Collection<FunctionSymbol> functions() {
        return Collections.unmodifiableCollection(this.functions);
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
        this.functions.add((FunctionSymbol) currentScope());
        super.visitFunctionDecl(ctx);
        popScope();
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
