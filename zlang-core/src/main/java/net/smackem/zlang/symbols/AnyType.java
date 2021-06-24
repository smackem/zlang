package net.smackem.zlang.symbols;

public enum AnyType implements Type {
    INSTANCE;

    @Override
    public String typeName() {
        return "any";
    }

    @Override
    public int byteSize() {
        throw new RuntimeException("type 'any' does not have a byte size");
    }

    @Override
    public RegisterType registerType() {
        throw new RuntimeException("type 'any' does not have a register type");
    }
}
