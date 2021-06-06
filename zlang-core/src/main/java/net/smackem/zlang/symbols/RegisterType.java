package net.smackem.zlang.symbols;

public interface RegisterType extends Type {
    RegisterTypeId id();
    boolean isReferenceType();
}
