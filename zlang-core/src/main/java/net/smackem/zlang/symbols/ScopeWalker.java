package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.lang.ZLangBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;

abstract class ScopeWalker extends ZLangBaseVisitor<Void> {
    private final Map<ParserRuleContext, Scope> scopes;
    private Scope currentScope;

    ScopeWalker(GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        this.currentScope = globalScope;
        this.scopes = scopes;
    }

    Scope currentScope() {
        return this.currentScope;
    }

    void enterScope(ParserRuleContext ctx) {
        final Scope scope = this.scopes.get(ctx);
        if (scope == null) {
            throw new RuntimeException("there is no scope for rule context " + ctx);
        }
        this.currentScope = scope;
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

    void popScope() {
        this.currentScope = this.currentScope.enclosingScope();
    }

    void defineSymbol(ParserRuleContext ctx, Scope scope, Symbol symbol) {
        try {
            scope.define(symbol.name(), symbol);
        } catch (CompilationErrorException e) {
            logSemanticError(ctx, e.getMessage());
        }
    }

    void logSemanticError(ParserRuleContext ctx, String message) {
        throw new RuntimeException(message);
    }
}
