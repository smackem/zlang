package net.smackem.zlang.symbols;

public interface Type {
    String typeName();
    int byteSize();
    RegisterType registerType();
}
