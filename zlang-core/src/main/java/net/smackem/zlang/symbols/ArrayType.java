package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Objects;

public class ArrayType extends AggregateTypeSymbol {
    private final Type elementType;

    public ArrayType(GlobalScope enclosingScope, Type elementType) {
        super(computeTypeName(elementType), enclosingScope);
        this.elementType = Objects.requireNonNull(elementType);
        defineBuiltInMethods();
    }

    public Type elementType() {
        return this.elementType;
    }

    @Override
    public String typeName() {
        return computeTypeName(this.elementType);
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

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        throw new UnsupportedOperationException("arrays cannot define new members");
    }

    private void defineBuiltInMethods() {
        try {
            defineBuiltInMethod(BuiltInTypeSymbol.INT, BuiltInFunction.ARRAY_LENGTH);
            defineBuiltInMethod(this, BuiltInFunction.ARRAY_COPY, BuiltInTypeSymbol.INT, BuiltInTypeSymbol.INT);
        } catch (CompilationErrorException e) {
            throw new RuntimeException(e); // duplicate identifier -> programming error
        }
    }

    private static String computeTypeName(Type elementType) {
        return "Array<" + elementType.typeName() + ">";
    }
}
