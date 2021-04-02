package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.SemanticErrorException;

public interface Scope {
    String scopeName();
    void define(String name, Symbol symbol) throws SemanticErrorException;
    Scope enclosingScope();
    Symbol resolve(String name);
}
