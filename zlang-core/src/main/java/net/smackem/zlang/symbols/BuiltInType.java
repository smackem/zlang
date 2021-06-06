package net.smackem.zlang.symbols;

import java.util.Arrays;
import java.util.Collection;

public enum BuiltInType {
    INT(new PrimitiveTypeSymbol("int", RegisterTypeId.Int32, false)),
    FLOAT(new PrimitiveTypeSymbol("float", RegisterTypeId.Float64, false)),
    BYTE(new PrimitiveTypeSymbol("byte", RegisterTypeId.Unsigned8, false)),
    OBJECT(new PrimitiveTypeSymbol("object", RegisterTypeId.Ref, true)),
    RUNTIME_PTR(new PrimitiveTypeSymbol("runtime_ptr", RegisterTypeId.NativePtr, false)),
    STRING(new StringType()),
    BOOL(new PrimitiveTypeSymbol("bool", RegisterTypeId.Int32, false));

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
}
