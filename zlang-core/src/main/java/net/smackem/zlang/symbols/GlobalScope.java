package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.SemanticErrorException;

public class GlobalScope implements Scope {

    private final SymbolTable symbolTable;

    GlobalScope() {
        this.symbolTable = new SymbolTable(null);
    }

    @Override
    public String scopeName() {
        return null;
    }

    @Override
    public void define(String name, Symbol symbol) throws SemanticErrorException {
        this.symbolTable.define(name, symbol);
    }

    @Override
    public Scope enclosingScope() {
        return null;
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol = this.symbolTable.resolve(name);
        if (symbol != null) {
            return symbol;
        }
        // walk all nested module scopes to resolve symbol
        for (final Symbol s : this.symbolTable.symbols()) {
            if (s instanceof ModuleSymbol) {
                symbol = ((ModuleSymbol) s).resolveMember(name);
                if (symbol != null) {
                    return symbol;
                }
            }
        }
        return null;
    }
}
