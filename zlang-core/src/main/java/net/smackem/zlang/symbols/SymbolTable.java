package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class SymbolTable implements MemberScope {
    private final String name;
    private final Scope enclosingScope;
    private final Map<String, Symbol> symbols = new HashMap<>();

    SymbolTable(Scope enclosingScope, String scopeName) {
        this.enclosingScope = enclosingScope;
        this.name = scopeName;
    }

    SymbolTable(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
        this.name = null;
    }

    public Collection<Symbol> symbols() {
        return Collections.unmodifiableCollection(this.symbols.values());
    }

    @Override
    public String scopeName() {
        return this.name;
    }

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        if (this.symbols.put(name, symbol) != null) {
            throw new CompilationErrorException("duplicate definition of symbol " + name);
        }
    }

    @Override
    public Scope enclosingScope() {
        return this.enclosingScope;
    }

    @Override
    public Symbol resolve(String name) {
        final Symbol symbol = this.symbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        if (this.enclosingScope != null) {
            return this.enclosingScope.resolve(name);
        }
        return null;
    }

    @Override
    public Symbol resolveMember(String name) {
        return this.symbols.get(name);
    }

    @Override
    public String toString() {
        return "SymbolTable" + symbols;
    }
}
