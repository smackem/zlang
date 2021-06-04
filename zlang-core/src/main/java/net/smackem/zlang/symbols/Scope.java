package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Collection;

public interface Scope {
    String scopeName();
    void define(String name, Symbol symbol) throws CompilationErrorException;
    Scope enclosingScope();
    Symbol resolve(String name);

    /**
     * @return all symbols defined in this scope, in the following order:
     * <ul>
     *     <li>types, in the order of definition</li>
     *     <li>functions and methods, in the order of definition</li>
     *     <li>variables, in the order of definition</li>
     * </ul>
     */
    Collection<Symbol> symbols();
}
