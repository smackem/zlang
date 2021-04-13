package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Collection;
import java.util.stream.Collectors;

public class FunctionSymbol extends Symbol implements Scope {
    private final SymbolTable symbolTable;
    private int localCount;

    public FunctionSymbol(String name, Type type, Scope enclosingScope) {
        super(name, type);
        this.symbolTable = new SymbolTable(enclosingScope, name);
    }

    @Override
    public String scopeName() {
        return this.symbolTable.scopeName();
    }

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        if (symbol instanceof ConstantSymbol == false) {
            throw new IllegalArgumentException("functions may only define constant symbols, not '" + symbol + "'");
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

    public int localCount() {
        return this.localCount;
    }

    void setLocalCount(int localCount) {
        this.localCount = localCount;
    }

    public boolean isEntryPoint() {
        return getClass() == FunctionSymbol.class
               && "main".equals(name())
               && type() == null
               && symbols().isEmpty();
    }

    @Override
    public String toString() {
        return "FunctionSymbol{" +
               "name=" + name() +
               ", returnType=" + type() +
               ", parameters=[" + this.symbolTable.symbols().stream()
                       .map(s -> s.name() + ':' + s.type().typeName())
                       .collect(Collectors.joining(", ")) +
               "]}";
    }
}
