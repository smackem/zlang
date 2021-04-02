package net.smackem.zlang.symbols;

public class VariableSymbol extends Symbol {
    VariableSymbol(String name, Type type) {
        super(name, type);
    }

    public boolean isAssignable() {
        return true;
    }
}
