package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Collection;

public class InterfaceSymbol extends Symbol implements Type, MemberScope {
    private final SymbolTable symbolTable;

    InterfaceSymbol(String name, Scope enclosingScope) {
        super(name, null);
        this.symbolTable = new SymbolTable(enclosingScope, name);
    }

    @Override
    public Symbol resolveMember(String name) {
        return this.symbolTable.resolveMember(name);
    }

    @Override
    public String scopeName() {
        return name();
    }

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        if (symbol instanceof InterfaceMethodSymbol == false) {
            throw new IllegalArgumentException("interfaces can only define interface methods, not '" + symbol + "'");
        }
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

    @Override
    public Collection<Symbol> symbols() {
        return this.symbolTable.symbols();
    }

    @Override
    public String typeName() {
        return name();
    }

    @Override
    public String toString() {
        return "InterfaceSymbol{" +
               "name=" + name() +
               ", symbolTable=" + symbolTable +
               '}';
    }

    @Override
    public int byteSize() {
        throw new UnsupportedOperationException("not implemented");
    }
}
