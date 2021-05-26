package net.smackem.zlang.symbols;

public enum NilType implements Type {
    INSTANCE;

    @Override
    public String typeName() {
        return "nil";
    }

    @Override
    public int byteSize() {
        return registerType().byteSize();
    }

    @Override
    public RegisterType registerType() {
        return BuiltInType.OBJECT.type();
    }
}
