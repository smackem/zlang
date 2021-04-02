package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.SemanticErrorException;

public final class BlockScope implements Scope {
    private final SymbolTable symbolTable;

    BlockScope(Scope enclosingScope) {
        this.symbolTable = new SymbolTable(enclosingScope);
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
