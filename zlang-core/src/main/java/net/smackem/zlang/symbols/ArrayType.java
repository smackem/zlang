package net.smackem.zlang.symbols;

import java.util.Objects;

public class ArrayType implements Type {
    private final Type elementType;

    public ArrayType(Type elementType) {
        this.elementType = Objects.requireNonNull(elementType);
    }

    public Type elementType() {
        return this.elementType;
    }

    @Override
    public String typeName() {
        return "Array<" + this.elementType.typeName() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ArrayType arrayType = (ArrayType) o;
        return elementType.equals(arrayType.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType);
    }

    @Override
    public String toString() {
        return "ArrayType{" +
               "elementType=" + elementType +
               '}';
    }

    @Override
    public int byteSize() {
        return BuiltInTypeSymbol.OBJECT.byteSize();
    }

    @Override
    public BuiltInTypeSymbol primitive() {
        return BuiltInTypeSymbol.OBJECT;
    }
}
