package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Objects;

public class ArrayType extends AggregateTypeSymbol {
    private final Type elementType;

    public ArrayType(Scope enclosingScope, Type elementType) {
        this(enclosingScope, elementType, typeName(elementType));
    }

    ArrayType(Scope enclosingScope, Type elementType, String typeName) {
        super(typeName, enclosingScope);
        this.elementType = Objects.requireNonNull(elementType);
        defineBuiltInMethods();
    }

    public Type elementType() {
        return this.elementType;
    }

    public static String typeName(Type elementType) {
        return "Array<" + elementType.typeName() + ">";
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
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        throw new UnsupportedOperationException("arrays cannot define new members");
    }

    @Override
    public int byteSize() {
        return BuiltInType.OBJECT.type().byteSize(); // dummy - array sizes are calculated inline on creation
    }

    private void defineBuiltInMethods() {
        try {
            defineBuiltInMethod(BuiltInType.INT.type(), BuiltInFunction.ARRAY_SIZE);
            defineBuiltInMethod(this, BuiltInFunction.ARRAY_COPY, BuiltInType.INT.type(), BuiltInType.INT.type());
        } catch (CompilationErrorException e) {
            throw new RuntimeException(e); // duplicate identifier -> programming error
        }
    }
}
