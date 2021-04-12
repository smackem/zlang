package net.smackem.zlang.symbols;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class BuiltInTypeSymbol extends Symbol implements Type {

    private final String typeName;
    private final int byteSize;
    private final TypeId id;

    public static final BuiltInTypeSymbol INT = new BuiltInTypeSymbol("int", 4, TypeId.Int32);
    public static final BuiltInTypeSymbol FLOAT = new BuiltInTypeSymbol("float", 8, TypeId.Float64);
    public static final BuiltInTypeSymbol BYTE = new BuiltInTypeSymbol("byte", 1, TypeId.Unsigned8);
    public static final BuiltInTypeSymbol OBJECT = new BuiltInTypeSymbol("object", 4, TypeId.Ref);
    public static final BuiltInTypeSymbol RUNTIME_PTR = new BuiltInTypeSymbol("runtime_ptr", 8, TypeId.NativePtr);
    public static final BuiltInTypeSymbol STRING = new BuiltInTypeSymbol("string", 4, TypeId.String);
    public static final BuiltInTypeSymbol BOOL = new BuiltInTypeSymbol("bool", 4, TypeId.Int32);

    public static Collection<BuiltInTypeSymbol> builtInTypes() {
        return List.of(INT, FLOAT, BYTE, OBJECT, RUNTIME_PTR, STRING, BOOL);
    }

    private BuiltInTypeSymbol(String typeName, int byteSize, TypeId id) {
        super(typeName, null);
        this.typeName = typeName;
        this.byteSize = byteSize;
        this.id = id;
    }

    public int id() {
        return this.id.id;
    }

    @Override
    public String typeName() {
        return this.typeName;
    }

    @Override
    public int byteSize() {
        return this.byteSize;
    }

    @Override
    public BuiltInTypeSymbol primitive() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BuiltInTypeSymbol that = (BuiltInTypeSymbol) o;
        return typeName.equals(that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }

    @Override
    public String toString() {
        return "BuiltInTypeSymbol{" +
                "typeName='" + typeName + '\'' +
                ", byteSize=" + byteSize +
                '}';
    }

    private enum TypeId {
        Void(0),
        Int32(1),
        Float64(2),
        Unsigned8(3),
        String(4),
        Ref(5),
        NativePtr(6);

        private int id;

        TypeId(int id) {
            this.id = id;
        }
    }
}
