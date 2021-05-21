package net.smackem.zlang.symbols;

public class BuiltInMethodSymbol extends MethodSymbol {
    BuiltInMethodSymbol(BuiltInFunction bif, Type type, MemberScope definingTypeScope) {
        super(bif.name(), type, definingTypeScope);
        setAddress(bif.address());
    }

    @Override
    public boolean isBuiltIn() {
        return true;
    }
}
