package net.smackem.zlang.symbols;

import java.util.Objects;

public class PrimitiveTypeSymbol extends Symbol implements Type, RegisterType {

    private final String typeName;
    private final RegisterTypeId typeId;

    PrimitiveTypeSymbol(String typeName, RegisterTypeId id) {
        super(typeName, null);
        this.typeName = typeName;
        this.typeId = id;
    }

    @Override
    public RegisterTypeId id() {
        return this.typeId;
    }

    @Override
    public String typeName() {
        return this.typeName;
    }

    @Override
    public int byteSize() {
        return this.typeId.byteSize();
    }

    @Override
    public RegisterType registerType() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PrimitiveTypeSymbol that = (PrimitiveTypeSymbol) o;
        return typeName.equals(that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }

    @Override
    public String toString() {
        return "PrimitiveTypeSymbol{" +
                "typeName='" + typeName + '\'' +
                ", registerTypeId=" + typeId +
                '}';
    }
}
