package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.ZLangParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;
import java.util.function.BiFunction;

class SymbolWalker extends ScopeWalker {

    SymbolWalker(GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        super(globalScope, scopes);
    }

    @Override
    public Void visitModule(ZLangParser.ModuleContext ctx) {
        enterScope(ctx);
        super.visitModule(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitFunctionDecl(ZLangParser.FunctionDeclContext ctx) {
        final String name = ctx.Ident().getText();
        final Type returnType = ctx.returnType() != null
                ? resolveType(ctx.returnType().type())
                : null;

        final FunctionSymbol functionSymbol;
        if (ctx.declaringTypePrefix() != null) {
            final String declTypeName = ctx.declaringTypePrefix().Ident().getText();
            final Symbol declType = currentScope().resolve(declTypeName);
            if (declType == null) {
                logSemanticError(ctx, "declaring type symbol not defined " + declTypeName);
                return null;
            }
            if (declType instanceof Type == false) {
                logSemanticError(ctx, "declaring type symbol is not a type " + declTypeName);
                return null;
            }
            if (declType instanceof MemberScope == false) {
                logSemanticError(ctx, "declaring type symbol is not a compound type " + declTypeName);
                return null;
            }
            functionSymbol = new MethodSymbol(name, returnType, (MemberScope) currentScope());
            defineSymbol(ctx, functionSymbol, new ConstantSymbol("self", (Type) declType));
            defineSymbol(ctx, (MemberScope) declType, functionSymbol);
        } else {
            functionSymbol = new FunctionSymbol(name, returnType, currentScope());
            defineSymbol(ctx, currentScope(), functionSymbol);
        }

        pushScope(ctx, functionSymbol);
        if (ctx.parameters() != null) {
            for (final var p : ctx.parameters().parameter()) {
                defineTypedIdent(p, ConstantSymbol::new);
            }
        }
        super.visitFunctionDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitBlock(ZLangParser.BlockContext ctx) {
        pushScope(ctx, new BlockScope(currentScope()));
        super.visitBlock(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitStructDecl(ZLangParser.StructDeclContext ctx) {
        enterScope(ctx);
        for (final var p : ctx.parameter()) {
            defineTypedIdent(p, (name, type) -> new FieldSymbol(name, type, (Type) currentScope()));
        }
        super.visitStructDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitUnionDecl(ZLangParser.UnionDeclContext ctx) {
        enterScope(ctx);
        for (final var p : ctx.parameter()) {
            defineTypedIdent(p, (name, type) -> new FieldSymbol(name, type, (Type) currentScope()));
        }
        super.visitUnionDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitForStmt(ZLangParser.ForStmtContext ctx) {
        pushScope(ctx, new BlockScope(currentScope())); // scope for declaring the iterator variable
        super.visitForStmt(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitForIteratorStmt(ZLangParser.ForIteratorStmtContext ctx) {
        defineTypedIdent(ctx.parameter(), ConstantSymbol::new);
        return super.visitForIteratorStmt(ctx);
    }

    @Override
    public Void visitForRangeStmt(ZLangParser.ForRangeStmtContext ctx) {
        defineTypedIdent(ctx.parameter(), ConstantSymbol::new);
        return super.visitForRangeStmt(ctx);
    }

    @Override
    public Void visitBindingStmt(ZLangParser.BindingStmtContext ctx) {
        defineTypedIdent(ctx.parameter(), ConstantSymbol::new);
        return super.visitBindingStmt(ctx);
    }

    @Override
    public Void visitVarDeclStmt(ZLangParser.VarDeclStmtContext ctx) {
        defineTypedIdent(ctx.parameter(), VariableSymbol::new);
        return super.visitVarDeclStmt(ctx);
    }

    private void defineTypedIdent(ZLangParser.ParameterContext ctx, BiFunction<String, Type, Symbol> symbolFactory) {
        final String ident = ctx.Ident().getText();
        final Type type = resolveType(ctx.type());
        final Symbol symbol = symbolFactory.apply(ident, type);
        defineSymbol(ctx, currentScope(), symbol);
    }

    private Type resolveType(ZLangParser.TypeContext ctx) {
        final String typeName = ctx.simpleType().getText();
        final Symbol innerTypeSymbol = currentScope().resolve(typeName);
        if (innerTypeSymbol == null) {
            logSemanticError(ctx, "type '" + typeName + "' not found!");
            return null;
        }
        if (innerTypeSymbol instanceof Type == false) {
            logSemanticError(ctx, "'" + typeName + "' is not a type!");
            return null;
        }
        if (ctx.LBracket() != null) {
            return new ArrayType((Type) innerTypeSymbol);
        }
        return (Type) innerTypeSymbol;
    }
}
