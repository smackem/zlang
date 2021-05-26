package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.Objects;

public class ListType extends AggregateTypeSymbol {
    private final ArrayType arrayType;

    public ListType(Scope enclosingScope, ArrayType innerArrayType) {
        super(typeName(innerArrayType.elementType()), enclosingScope);
        this.arrayType = Objects.requireNonNull(innerArrayType);
        defineMembers();
    }

    public ArrayType arrayType() {
        return this.arrayType;
    }

    public FieldSymbol sizeField() {
        return (FieldSymbol) resolveMember("@size");
    }

    public FieldSymbol arrayField() {
        return (FieldSymbol) resolveMember("@array");
    }

    public static String typeName(Type elementType) {
        return "List<" + elementType.typeName() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ListType listType = (ListType) o;
        return Objects.equals(arrayType, listType.arrayType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrayType);
    }

    @Override
    public String toString() {
        return "ListType{" +
               "elementType=" + this.arrayType.elementType() +
               '}';
    }

    @Override
    public int byteSize() {
        return sumFieldSizes();
    }

    private void defineMembers() {
        final Type elementType = this.arrayType.elementType();
        try {
            defineBuiltInFields(
                    /* 0 */ new FieldSymbol("@size", BuiltInType.INT.type(), this),
                    /* 4 */ new FieldSymbol("@array", this.arrayType, this));
            defineBuiltInMethod(BuiltInType.INT.type(), BuiltInFunction.LIST_SIZE);
            defineBuiltInMethod(BuiltInType.INT.type(), BuiltInFunction.LIST_CAPACITY);
            defineBuiltInMethod(null, BuiltInFunction.LIST_ADD, elementType);
            defineBuiltInMethod(null, BuiltInFunction.LIST_REMOVE, BuiltInType.INT.type());
            defineBuiltInMethod(null, BuiltInFunction.LIST_SET, BuiltInType.INT.type(), elementType);
            defineBuiltInMethod(elementType, BuiltInFunction.LIST_GET, BuiltInType.INT.type());
        } catch (CompilationErrorException e) {
            throw new RuntimeException(e); // duplicate identifier -> programming error
        }
    }
}
