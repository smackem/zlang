package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.lang.ZLangParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;
import java.util.Objects;

public abstract class ScopeWalker<T> extends ZLangBaseVisitor<T> {
    private final String moduleName;
    private final Map<ParserRuleContext, Scope> scopes;
    private final GlobalScope globalScope;
    private Scope currentScope;

    protected ScopeWalker(String moduleName, GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        this.moduleName = Objects.requireNonNull(moduleName);
        this.globalScope = globalScope;
        this.currentScope = globalScope;
        this.scopes = scopes;
    }

    protected final String moduleName() {
        return this.moduleName;
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
        throw new RuntimeException("%s %d:%d %s".formatted(
                this.moduleName, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), message));
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
            symbol.setDefiningScope(scope);
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
        for (final var postfix : ctx.typePostfix()) {
            if (postfix.LBracket() != null) {
                type = defineArrayType(type);
            } else if (postfix.List() != null) {
                type = defineListType(type);
            } else {
                throw new UnsupportedOperationException("unknown type postfix");
            }
        }
        return type;
    }

    protected ArrayType defineArrayType(Type elementType) {
        final String arrayTypeName = ArrayType.typeName(elementType);
        final Symbol resolvedListType = currentScope().resolve(arrayTypeName);
        if (resolvedListType != null) {
            return (ArrayType) resolvedListType;
        }
        final ArrayType type = new ArrayType(this.globalScope, elementType);
        try {
            globalScope().define(arrayTypeName, type);
        } catch (CompilationErrorException e) {
            throw new RuntimeException(e); // must not happen
        }
        return type;
    }

    protected ListType defineListType(Type elementType) {
        final String listTypeName = ListType.typeName(elementType);
        final Symbol resolvedListType = currentScope().resolve(listTypeName);
        if (resolvedListType != null) {
            return (ListType) resolvedListType;
        }
        final ListType type = new ListType(this.globalScope, defineArrayType(elementType));
        try {
            globalScope().define(listTypeName, type);
        } catch (CompilationErrorException e) {
            throw new RuntimeException(e); // must not happen
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
