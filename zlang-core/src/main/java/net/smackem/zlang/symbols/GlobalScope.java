package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Collection;

public class GlobalScope implements Scope {
    private final SymbolTable symbolTable;

    GlobalScope() {
        this.symbolTable = new SymbolTable(null);
        initTypeSystem();
    }

    @Override
    public String scopeName() {
        return null;
    }

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
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

    @Override
    public Collection<Symbol> symbols() {
        return this.symbolTable.symbols();
    }

    private void initTypeSystem() {
        try {
            for (final Symbol typeSymbol : BuiltInTypeSymbol.builtInTypes()) {
                define(typeSymbol.name(), typeSymbol);
            }
        } catch (CompilationErrorException ignored) {
            // cannot happen for built-in types
        }
    }

    @Override
    public String toString() {
        return "GlobalScope{" +
               "scopeName=" + scopeName() +
               ", symbolTable=" + symbolTable +
               '}';
    }
}