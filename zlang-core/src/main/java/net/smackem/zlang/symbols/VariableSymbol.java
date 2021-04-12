package net.smackem.zlang.symbols;

public class VariableSymbol extends Symbol {
    private final boolean global;

    VariableSymbol(String name, Type type, boolean global) {
        super(name, type);
        this.global = global;
    }

    @Override
    public String toString() {
        return "VariableSymbol{name=%s, type=%s}".formatted(name(), type());
    }

    public boolean isAssignable() {
        return true;
    }

    public final boolean isGlobal() {
        return this.global;
    }
}
