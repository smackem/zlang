package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.*;
import java.util.stream.Collectors;

class SymbolTable implements MemberScope {
    private final String name;
    private final Scope enclosingScope;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();

    SymbolTable(Scope enclosingScope, String scopeName) {
        this.enclosingScope = enclosingScope;
        this.name = scopeName;
    }

    SymbolTable(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
        this.name = null;
    }

    @Override
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
        return "SymbolTable{name='%s', symbols=%s}".formatted(
                this.name,
                symbols.values().stream()
                        .map(s -> s.name() + ": " + (s.type() != null ? s.type().typeName() : null))
                        .collect(Collectors.joining(", ")));
    }
}
