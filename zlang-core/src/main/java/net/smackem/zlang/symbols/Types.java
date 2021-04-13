package net.smackem.zlang.symbols;

import java.util.Objects;

public final class Types {
    private Types() { }

    public static boolean isAssignable(Type lvalue, Type rvalue) {
        if (Objects.equals(lvalue, rvalue)) {
            return true;
        }
        if (lvalue.primitive() == BuiltInTypeSymbol.OBJECT && rvalue == NilType.INSTANCE) {
            return true;
        }
        if (lvalue == BuiltInTypeSymbol.OBJECT && rvalue.primitive() == BuiltInTypeSymbol.OBJECT) {
            return true;
        }
        if (lvalue == BuiltInTypeSymbol.INT) {
            return rvalue == BuiltInTypeSymbol.BOOL || rvalue == BuiltInTypeSymbol.BYTE;
        }
        return false;
    }
}
