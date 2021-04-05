package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

public class UnionSymbol extends Symbol implements Type, MemberScope {
    private final SymbolTable symbolTable;

    UnionSymbol(String name, Scope enclosingScope) {
        super(name, null);
        this.symbolTable = new SymbolTable(enclosingScope);
    }

    @Override
    public String typeName() {
        return name();
    }

    @Override
    public Symbol resolveMember(String name) {
        return this.symbolTable.resolveMember(name);
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

    @Override
    public String toString() {
        return "StructSymbol{" +
               "name=" + name() +
               ", symbolTable=" + symbolTable +
               '}';
    }
}

