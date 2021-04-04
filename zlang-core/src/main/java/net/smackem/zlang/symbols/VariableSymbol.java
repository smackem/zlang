package net.smackem.zlang.symbols;

public class VariableSymbol extends Symbol {
    VariableSymbol(String name, Type type) {
        super(name, type);
    }

    @Override
    public String toString() {
        return "VariableSymbol{name=%s, type=%s}".formatted(name(), type());
    }

    public boolean isAssignable() {
        return true;
    }
}
