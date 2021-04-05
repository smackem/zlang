package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

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
    public void define(String name, Symbol symbol) throws CompilationErrorException {
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
