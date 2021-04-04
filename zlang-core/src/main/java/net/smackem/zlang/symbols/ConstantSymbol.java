package net.smackem.zlang.symbols;

public class ConstantSymbol extends VariableSymbol {
    ConstantSymbol(String name, Type type) {
        super(name, type);
    }

    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public String toString() {
        return "ConstantSymbol{name=%s, type=%s}".formatted(name(), type());
    }
}
