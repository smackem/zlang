package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.lang.ZLangParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.HashMap;
import java.util.Map;

public class SymbolWalker extends ZLangBaseVisitor<Void> {
    private final Map<ParserRuleContext, Scope> scopes = new HashMap<>();
    private final String fileName;
    private Scope currentScope;

    public SymbolWalker(String fileName, GlobalScope globalScope) {
        this.fileName = fileName;
        this.currentScope = globalScope;
    }

    public Map<ParserRuleContext, Scope> symbolMap() {
        return this.scopes;
    }

    @Override
    public Void visitProgram(ZLangParser.ProgramContext ctx) {
        pushScope(ctx, new ModuleSymbol(this.fileName, this.currentScope));
        return super.visitProgram(ctx);
    }

    @Override
    public Void visitFunctionDecl(ZLangParser.FunctionDeclContext ctx) {
        return super.visitFunctionDecl(ctx);
    }

    @Override
    public Void visitBlock(ZLangParser.BlockContext ctx) {
        return super.visitBlock(ctx);
    }

    @Override
    public Void visitStructDecl(ZLangParser.StructDeclContext ctx) {
        return super.visitStructDecl(ctx);
    }

    @Override
    public Void visitBindingStmt(ZLangParser.BindingStmtContext ctx) {
        return super.visitBindingStmt(ctx);
    }

    @Override
    public Void visitParameter(ZLangParser.ParameterContext ctx) {
        return super.visitParameter(ctx);
    }

    @Override
    public Void visitVarDeclStmt(ZLangParser.VarDeclStmtContext ctx) {
        return super.visitVarDeclStmt(ctx);
    }

    @Override
    public Void visitForStmt(ZLangParser.ForStmtContext ctx) {
        return super.visitForStmt(ctx);
    }

    private void pushScope(ParserRuleContext ctx, Scope scope) {
        if (scope.enclosingScope() != this.currentScope) {
            throw new RuntimeException("new scope is not enclosed by current scope");
        }
        this.scopes.put(ctx, scope);
        this.currentScope = scope;
    }
}
