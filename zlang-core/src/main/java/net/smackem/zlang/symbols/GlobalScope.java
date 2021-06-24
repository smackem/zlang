package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Collection;

public class GlobalScope implements Scope {
    private final SymbolTable symbolTable;

    public GlobalScope() {
        this.symbolTable = new SymbolTable(null);
        defineBuiltInSymbols();
    }

    @Override
    public String scopeName() {
        return null;
    }

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        this.symbolTable.define(name, symbol);
    }

    @Override
    public Scope enclosingScope() {
        return null;
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol = this.symbolTable.resolve(name);
        if (symbol != null) {
            return symbol;
        }
        // walk all nested module scopes to resolve symbol
        for (final Symbol s : this.symbolTable.symbols()) {
            if (s instanceof ModuleSymbol ms) {
                symbol = ms.resolveMember(name);
                if (symbol != null) {
                    return symbol;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<Symbol> symbols() {
        return this.symbolTable.symbols();
    }

    private void defineBuiltInSymbols() {
        try {
            for (final Type t : BuiltInType.builtInTypes()) {
                final Symbol symbol = (Symbol) t;
                define(symbol.name(), symbol);
            }
            defineBuiltInFunction(null, BuiltInFunction.PRINT, AnyType.INSTANCE, BuiltInType.INT.type());
        } catch (CompilationErrorException ignored) {
            // cannot happen for built-in types
        }
    }

    private void defineBuiltInFunction(Type returnType, BuiltInFunction bif, Type... parameterTypes) throws CompilationErrorException {
        final BuiltInFunctionSymbol function = new BuiltInFunctionSymbol(bif, returnType, this);
        int parameterIndex = 0;
        for (final Type parameterType : parameterTypes) {
            final String parameterName = "p" + parameterIndex++;
            function.define(parameterName, new ConstantSymbol(parameterName, parameterType, false));
        }
        this.symbolTable.define(bif.ident(), function);
    }

    @Override
    public String toString() {
        return "GlobalScope{" +
               "scopeName=" + scopeName() +
               ", symbolTable=" + symbolTable +
               '}';
    }
}
