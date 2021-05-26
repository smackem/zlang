package net.smackem.zlang.symbols;

import java.util.Objects;

public final class Types {
    private Types() { }

    public static boolean isAssignable(Type lvalue, Type rvalue) {
        if (Objects.equals(lvalue, rvalue)) {
            return true;
        }
        if (lvalue.registerType() == BuiltInType.OBJECT.type() && rvalue == NilType.INSTANCE) {
            return true;
        }
        if (lvalue.registerType() == BuiltInType.STRING.type() && rvalue == NilType.INSTANCE) {
            return true;
        }
        if (lvalue == BuiltInType.OBJECT.type() && rvalue.registerType() == BuiltInType.OBJECT.type()) {
            return true;
        }
        if (lvalue == BuiltInType.INT.type()) {
            return rvalue == BuiltInType.BOOL.type() || rvalue == BuiltInType.BYTE.type();
        }
        return false;
    }
}
