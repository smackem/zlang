package net.smackem.zlang.symbols;

public class ArrayType implements Type {
    private final Type elementType;

    ArrayType(Type elementType) {
        this.elementType = elementType;
    }

    public Type elementType() {
        return this.elementType;
    }

    @Override
    public String typeName() {
        return "Array<" + this.elementType.typeName() + ">";
    }
}
