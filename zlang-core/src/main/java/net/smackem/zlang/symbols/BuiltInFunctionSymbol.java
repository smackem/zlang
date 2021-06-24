package net.smackem.zlang.symbols;

public class BuiltInFunctionSymbol extends FunctionSymbol {
    public BuiltInFunctionSymbol(BuiltInFunction bif, Type type, Scope enclosingScope) {
        super(bif.ident(), type, enclosingScope);
        setAddress(bif.address());
    }

    @Override
    public boolean isBuiltIn() {
        return true;
    }
}
