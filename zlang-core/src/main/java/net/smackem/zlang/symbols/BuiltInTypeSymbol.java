package net.smackem.zlang.symbols;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class BuiltInTypeSymbol extends Symbol implements Type {

    private final String typeName;
    private final int byteSize;

    public static final BuiltInTypeSymbol INT = new BuiltInTypeSymbol("int", 4);
    public static final BuiltInTypeSymbol FLOAT = new BuiltInTypeSymbol("float", 8);
    public static final BuiltInTypeSymbol BYTE = new BuiltInTypeSymbol("byte", 1);
    public static final BuiltInTypeSymbol OBJECT = new BuiltInTypeSymbol("object", 4);
    public static final BuiltInTypeSymbol RUNTIME_PTR = new BuiltInTypeSymbol("runtime_ptr", 8);
    public static final BuiltInTypeSymbol STRING = new BuiltInTypeSymbol("string", 4);
    public static final BuiltInTypeSymbol BOOL = new BuiltInTypeSymbol("bool", 4);

    public static Collection<BuiltInTypeSymbol> builtInTypes() {
        return List.of(INT, FLOAT, BYTE, OBJECT, RUNTIME_PTR, STRING, BOOL);
    }

    private BuiltInTypeSymbol(String typeName, int byteSize) {
        super(typeName, null);
        this.typeName = typeName;
        this.byteSize = byteSize;
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
               '}';
    }
}
