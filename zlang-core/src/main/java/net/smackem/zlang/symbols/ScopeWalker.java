package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.lang.ZLangParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;

public abstract class ScopeWalker<T> extends ZLangBaseVisitor<T> {
    private final Map<ParserRuleContext, Scope> scopes;
    private final GlobalScope globalScope;
    private Scope currentScope;

    protected ScopeWalker(GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        this.globalScope = globalScope;
        this.currentScope = globalScope;
        this.scopes = scopes;
    }

    protected final Scope currentScope() {
        return this.currentScope;
    }

    protected final GlobalScope globalScope() {
        return this.globalScope;
    }

    protected void enterScope(ParserRuleContext ctx) {
        final Scope scope = this.scopes.get(ctx);
        if (scope == null) {
            throw new RuntimeException("there is no scope for rule context " + ctx);
        }
        this.currentScope = scope;
    }

    protected void popScope() {
        this.currentScope = this.currentScope.enclosingScope();
    }

    protected void logSemanticError(ParserRuleContext ctx, String message) {
        throw new RuntimeException("%d:%d %s".formatted(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), message));
    }

    void pushScope(ParserRuleContext ctx, Scope scope) {
        if (scope.enclosingScope() != this.currentScope) {
            throw new RuntimeException("new scope is not enclosed by current scope");
        }
        if (this.scopes.put(ctx, scope) != null) {
            throw new RuntimeException("there already was a scope for rule context " + ctx);
        }
        this.currentScope = scope;
    }

    void defineSymbol(ParserRuleContext ctx, Scope scope, Symbol symbol) {
        try {
            scope.define(symbol.name(), symbol);
            symbol.setLineNumber(ctx.getStart().getLine());
            symbol.setCharPosition(ctx.getStart().getCharPositionInLine());
        } catch (CompilationErrorException e) {
            logSemanticError(ctx, e.getMessage());
        }
    }

    protected Type resolveType(ZLangParser.TypeContext ctx) {
        final String typeName = ctx.simpleType().getText();
        Type type = resolveType(ctx, typeName);
        if (type == null) {
            return null;
        }
        for (final var ignored : ctx.LBracket()) {
            type = new ArrayType(this.globalScope, type);
        }
        return type;
    }

    protected Type resolveType(ParserRuleContext ctx, String ident) {
        final Symbol innerTypeSymbol = currentScope().resolve(ident);
        if (innerTypeSymbol == null) {
            logSemanticError(ctx, "type '" + ident + "' not found!");
            return null;
        }
        if (innerTypeSymbol instanceof Type == false) {
            logSemanticError(ctx, "'" + ident + "' is not a type!");
            return null;
        }
        return (Type) innerTypeSymbol;
    }
}
