package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

public interface Scope {
    String scopeName();
    void define(String name, Symbol symbol) throws CompilationErrorException;
    Scope enclosingScope();
    Symbol resolve(String name);
}
