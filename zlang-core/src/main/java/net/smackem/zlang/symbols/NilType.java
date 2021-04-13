package net.smackem.zlang.symbols;

public enum NilType implements Type {
    INSTANCE;

    @Override
    public String typeName() {
        return "nil";
    }

    @Override
    public int byteSize() {
        return primitive().byteSize();
    }

    @Override
    public BuiltInTypeSymbol primitive() {
        return BuiltInTypeSymbol.OBJECT;
    }
}
