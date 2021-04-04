package net.smackem.zlang.symbols;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class BuiltInTypeSymbol extends Symbol implements Type {

    public static final BuiltInTypeSymbol INT = new BuiltInTypeSymbol("int");
    public static final BuiltInTypeSymbol FLOAT = new BuiltInTypeSymbol("float");
    public static final BuiltInTypeSymbol BYTE = new BuiltInTypeSymbol("byte");
    public static final BuiltInTypeSymbol ANY = new BuiltInTypeSymbol("any");
    public static final BuiltInTypeSymbol RUNTIME_PTR = new BuiltInTypeSymbol("runtime_ptr");
    public static final BuiltInTypeSymbol STRING = new BuiltInTypeSymbol("string");

    public static Collection<BuiltInTypeSymbol> builtInTypes() {
        return List.of(INT, FLOAT, BYTE, ANY, RUNTIME_PTR, STRING);
    }

    private final String typeName;

    private BuiltInTypeSymbol(String typeName) {
        super(typeName, null);
        this.typeName = typeName;
    }

    @Override
    public String typeName() {
        return this.typeName;
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
