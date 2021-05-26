package net.smackem.zlang.symbols;

import java.util.Arrays;
import java.util.Collection;

public enum BuiltInType {
    INT(new PrimitiveTypeSymbol("int", RegisterTypeId.Int32)),
    FLOAT(new PrimitiveTypeSymbol("float", RegisterTypeId.Float64)),
    BYTE(new PrimitiveTypeSymbol("byte", RegisterTypeId.Unsigned8)),
    OBJECT(new PrimitiveTypeSymbol("object", RegisterTypeId.Ref)),
    RUNTIME_PTR(new PrimitiveTypeSymbol("runtime_ptr", RegisterTypeId.NativePtr)),
    STRING(new StringType()),
    BOOL(new PrimitiveTypeSymbol("bool", RegisterTypeId.Int32));

    private final RegisterType type;

    BuiltInType(RegisterType type) {
        this.type = type;
    }

    public RegisterType type() {
        return this.type;
    }

    public static Collection<? extends Type> builtInTypes() {
        return Arrays.stream(values())
                .map(BuiltInType::type)
                .toList();
    }

    public static RegisterType fromId(int id) {
        return Arrays.stream(values())
                .filter(v -> v.type.id().number() == id)
                .findFirst()
                .map(v -> v.type)
                .orElseThrow();
    }
}
