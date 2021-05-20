package net.smackem.zlang.symbols;

import java.util.Arrays;

public class BuiltInMethodSymbol extends MethodSymbol {
    BuiltInMethodSymbol(String name, Type type, MemberScope enclosingScope, int address) {
        super(name, type, enclosingScope);
        setAddress(address);
    }

    @Override
    public void setAddress(int address) {
        // check that address is one of the known addresses for built-in functions
        Arrays.stream(BuiltInFunction.values())
                .filter(v -> v.address() == address)
                .findFirst()
                .orElseThrow();
        super.setAddress(address);
    }
}
