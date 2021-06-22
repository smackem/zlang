package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.ZLangParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;
import java.util.function.BiFunction;

class SymbolWalker extends ScopeWalker<Void> {
    private int globalSegmentSize = 0;

    SymbolWalker(GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes, int globalSegmentSize) {
        super(globalScope, scopes);
        this.globalSegmentSize = globalSegmentSize;
    }

    public int globalSegmentSize() {
        return this.globalSegmentSize;
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
        int paramRegisterNumber;
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
            if (declType instanceof InterfaceSymbol) {
                logSemanticError(ctx, "interface methods must not be implemented " + declTypeName);
                return null;
            }
            paramRegisterNumber = 2;
            functionSymbol = new MethodSymbol(name, returnType, (MemberScope) currentScope());
            final Symbol self = new SelfSymbol((Type) declType);
            self.setAddress(1);
            defineSymbol(ctx, functionSymbol, self);
            defineSymbol(ctx, (MemberScope) declType, functionSymbol);
        } else {
            paramRegisterNumber = 1;
            functionSymbol = new FunctionSymbol(name, returnType, currentScope());
            defineSymbol(ctx, currentScope(), functionSymbol);
        }

        pushScope(ctx, functionSymbol);
        if (ctx.parameters() != null) {
            for (final var p : ctx.parameters().parameter()) {
                final var symbol = defineTypedIdent(p, (ident, type) -> new ConstantSymbol(ident, type, false));
                symbol.setAddress(paramRegisterNumber);
                paramRegisterNumber++;
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
    public Void visitBlockExpr(ZLangParser.BlockExprContext ctx) {
        pushScope(ctx, new BlockScope(currentScope()));
        super.visitBlockExpr(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitStructDecl(ZLangParser.StructDeclContext ctx) {
        enterScope(ctx);
        int fieldAddr = 0;
        for (final var p : ctx.parameter()) {
            final var symbol = defineTypedIdent(p, (name, type) -> new FieldSymbol(name, type, (Type) currentScope()));
            symbol.setAddress(fieldAddr);
            if (symbol.type().byteSize() == 0) {
                logSemanticError(p, "reference to undefined type " + symbol.type());
            }
            fieldAddr += symbol.type().registerType().byteSize();
        }
        assert fieldAddr == ((Type) currentScope()).byteSize();
        super.visitStructDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitUnionDecl(ZLangParser.UnionDeclContext ctx) {
        enterScope(ctx);
        final FieldSymbol flagField = ((UnionSymbol) currentScope()).flagField();
        final int flagFieldSize = flagField.type().byteSize();
        if (ctx.unionParameter().size() > UnionSymbol.MAX_FIELDS) {
            logSemanticError(ctx, "unions may only contain up to %d fields!".formatted(UnionSymbol.MAX_FIELDS));
            return null;
        }
        for (final var p : ctx.unionParameter()) {
            final var symbol = defineTypedIdent(p, (name, type) -> new FieldSymbol(name, type, (Type) currentScope()));
            symbol.setAddress(flagFieldSize);
            if (symbol.type().byteSize() == 0) {
                logSemanticError(p, "reference to undefined type " + symbol.type());
            }
            assert symbol.type().byteSize() > 0;
        }
        super.visitUnionDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitInterfaceDecl(ZLangParser.InterfaceDeclContext ctx) {
        enterScope(ctx);
        super.visitInterfaceDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitInterfaceMethodDecl(ZLangParser.InterfaceMethodDeclContext ctx) {
        final String name = ctx.Ident().getText();
        final Type returnType = ctx.returnType() != null
                ? resolveType(ctx.returnType().type())
                : null;
        super.visitInterfaceMethodDecl(ctx);
        final Type interfaceType = (Type) currentScope();
        final InterfaceMethodSymbol imd = new InterfaceMethodSymbol(name, returnType, (MemberScope) currentScope());
        defineSymbol(ctx, currentScope(), imd);

        pushScope(ctx, imd);
        final Symbol self = new SelfSymbol(interfaceType);
        self.setAddress(1);
        defineSymbol(ctx, imd, self);
        if (ctx.parameters() != null) {
            for (final var p : ctx.parameters().parameter()) {
                defineTypedIdent(p, (ident, type) -> new ConstantSymbol(ident, type, false));
            }
        }
        super.visitInterfaceMethodDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitImplementsClause(ZLangParser.ImplementsClauseContext ctx) {
        final AggregateTypeSymbol aggregate = (AggregateTypeSymbol) currentScope();
        for (final var ident : ctx.Ident()) {
            final String id = ident.getText();
            final Symbol resolvedSymbol = currentScope().resolve(id);
            if (resolvedSymbol == null) {
                logSemanticError(ctx, "undeclared symbol '" + id + "'");
            }
            if (resolvedSymbol instanceof InterfaceSymbol ifc) {
                aggregate.addImplementedInterface(ifc);
            } else {
                logSemanticError(ctx, "symbol '" + id + "' is not an interface, but " + resolvedSymbol);
            }
        }
        return super.visitImplementsClause(ctx);
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
        final var symbol = defineTypedIdent(ctx.parameter(),
                (ident, type) -> new ConstantSymbol(ident, type, false));
        addLocal(symbol);
        return super.visitForIteratorStmt(ctx);
    }

    @Override
    public Void visitForRangeStmt(ZLangParser.ForRangeStmtContext ctx) {
        final var symbol = defineTypedIdent(ctx.parameter(),
                (ident, type) -> new ConstantSymbol(ident, type, false));
        addLocal(symbol);
        return super.visitForRangeStmt(ctx);
    }

    @Override
    public Void visitBindingStmt(ZLangParser.BindingStmtContext ctx) {
        final boolean global = enclosingFunction() == null;
        final var symbol = defineTypedIdent(ctx.parameter(),
                (ident, type) -> new ConstantSymbol(ident, type, global));
        if (global) {
            symbol.setAddress(incrementGlobalSegmentSize(symbol));
        } else {
            addLocal(symbol);
        }
        return super.visitBindingStmt(ctx);
    }

    @Override
    public Void visitVarDeclStmt(ZLangParser.VarDeclStmtContext ctx) {
        final boolean global = enclosingFunction() == null;
        final var symbol = defineTypedIdent(ctx.parameter(),
                (ident, type) -> new VariableSymbol(ident, type, global));
        if (global) {
            symbol.setAddress(incrementGlobalSegmentSize(symbol));
        } else {
            addLocal(symbol);
        }
        return super.visitVarDeclStmt(ctx);
    }

    @Override
    public Void visitSwitchUnionFieldClause(ZLangParser.SwitchUnionFieldClauseContext ctx) {
        pushScope(ctx, new BlockScope(currentScope())); // scope for declaring the iterator variable
        final var symbol = defineTypedIdent(ctx.unionParameter(),
                (ident, type) -> new ConstantSymbol(ident, type, false));
        addLocal(symbol);
        super.visitSwitchUnionFieldClause(ctx);
        popScope();
        return null;
    }

    private <T extends Symbol> T defineTypedIdent(ZLangParser.ParameterContext ctx, BiFunction<String, Type, T> symbolFactory) {
        final String ident = ctx.Ident().getText();
        final Type type = resolveType(ctx.type());
        final T symbol = symbolFactory.apply(ident, type);
        defineSymbol(ctx, currentScope(), symbol);
        return symbol;
    }

    private <T extends Symbol> T defineTypedIdent(ZLangParser.UnionParameterContext ctx, BiFunction<String, Type, T> symbolFactory) {
        final String ident = ctx.Ident().getText();
        final Type type = ctx.Void() != null ? NilType.INSTANCE : resolveType(ctx.type());
        final T symbol = symbolFactory.apply(ident, type);
        defineSymbol(ctx, currentScope(), symbol);
        return symbol;
    }

    private void addLocal(VariableSymbol variable) {
        final FunctionSymbol declaringFunction = enclosingFunction();
        assert declaringFunction != null;
        declaringFunction.addLocal(variable);
    }

    private FunctionSymbol enclosingFunction() {
        return Scopes.enclosingSymbol(currentScope(), FunctionSymbol.class);
    }

    private int incrementGlobalSegmentSize(Symbol symbol) {
        int size = this.globalSegmentSize;
        this.globalSegmentSize += symbol.type().registerType().byteSize();
        return size;
    }
}
