package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Objects;

public class ArrayType extends AggregateTypeSymbol {
    private final Type elementType;

    public ArrayType(Scope enclosingScope, Type elementType) {
        super(typeName(elementType), enclosingScope);
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
        return BuiltInTypeSymbol.OBJECT.byteSize(); // dummy - array sizes are calculated inline on creation
    }

    private void defineBuiltInMethods() {
        try {
            defineBuiltInMethod(BuiltInTypeSymbol.INT, BuiltInFunction.ARRAY_SIZE);
            defineBuiltInMethod(this, BuiltInFunction.ARRAY_COPY, BuiltInTypeSymbol.INT, BuiltInTypeSymbol.INT);
        } catch (CompilationErrorException e) {
            throw new RuntimeException(e); // duplicate identifier -> programming error
        }
    }
}
