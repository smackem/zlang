package net.smackem.zlang.symbols;

public class FieldSymbol extends VariableSymbol {
    private final Type declaringType;

    public FieldSymbol(String name, Type type, Type declaringType) {
        super(name, type);
        this.declaringType = declaringType;
    }

    public Type declaringType() {
        return this.declaringType;
    }

    @Override
    public String toString() {
        return "FieldSymbol{" +
               "name=" + name() +
               ", type=" + type() +
               ", declaringType=" + declaringType +
               '}';
    }
}
