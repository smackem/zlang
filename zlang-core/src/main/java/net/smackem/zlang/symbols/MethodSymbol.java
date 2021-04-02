package net.smackem.zlang.symbols;

public class MethodSymbol extends FunctionSymbol {
    private final Type declaringType;

    MethodSymbol(String name, Type type, Scope enclosingScope, Type declaringType) {
        super(name, type, enclosingScope);
        this.declaringType = declaringType;
    }

    public Type declaringType() {
        return this.declaringType;
    }
}
