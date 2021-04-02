package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.SemanticErrorException;

public class FunctionSymbol extends Symbol implements Scope {
    private final SymbolTable symbolTable;

    FunctionSymbol(String name, Type type, Scope enclosingScope) {
        super(name, type);
        this.symbolTable = new SymbolTable(enclosingScope, name);
    }

    @Override
    public String scopeName() {
        return this.symbolTable.scopeName();
    }

    @Override
    public void define(String name, Symbol symbol) throws SemanticErrorException {
        this.symbolTable.define(name, symbol);
    }

    @Override
    public Scope enclosingScope() {
        return this.symbolTable.enclosingScope();
    }

    @Override
    public Symbol resolve(String name) {
        return this.symbolTable.resolve(name);
    }
}
