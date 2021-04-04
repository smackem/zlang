package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.SemanticErrorException;

public class ModuleSymbol extends Symbol implements MemberScope {
    private final SymbolTable symbolTable;

    ModuleSymbol(String name, Scope enclosingScope) {
        super(name, null);
        this.symbolTable = new SymbolTable(enclosingScope);
    }

    @Override
    public String scopeName() {
        return name();
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

    @Override
    public Symbol resolveMember(String name) {
        return this.symbolTable.resolveMember(name);
    }

    @Override
    public String toString() {
        return "ModuleSymbol{" +
               "name=" + name() +
               ", symbolTable=" + symbolTable +
               '}';
    }
}
