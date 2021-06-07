package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionSymbol extends Symbol implements Scope {
    private final SymbolTable symbolTable;
    private final List<VariableSymbol> locals = new ArrayList<>();

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
        return this.locals.size();
    }

    public Collection<VariableSymbol> locals() {
        return Collections.unmodifiableCollection(this.locals);
    }

    void addLocal(VariableSymbol symbol) {
        final int address = this.symbolTable.symbols().size() + this.locals.size() + 1; // locals start at 1 + paramCount
        symbol.setAddress(address);
        this.locals.add(symbol);
    }

    public boolean isEntryPoint() {
        return getClass() == FunctionSymbol.class
               && "main".equals(name())
               && type() == null
               && symbols().isEmpty();
    }

    public boolean isBuiltIn() {
        return false;
    }

    public boolean signatureMatches(FunctionSymbol other) {
        if (Objects.equals(other.name(), name()) == false) {
            return false;
        }
        if (Objects.equals(other.type(), type()) == false) {
            return false;
        }
        if (symbols().size() != other.symbols().size()) {
            return false;
        }
        final Iterator<Symbol> xParam = symbols().iterator();
        final Iterator<Symbol> yParam = other.symbols().iterator();
        while (xParam.hasNext() && yParam.hasNext()) {
            final Symbol x = xParam.next();
            final Symbol y = yParam.next();
            if (x instanceof SelfSymbol && y instanceof SelfSymbol) {
                // skip self because it will not have identical types
                continue;
            }
            if (Objects.equals(x.type(), y.type()) == false) {
                return false;
            }
        }
        return true;
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
