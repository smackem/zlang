package net.smackem.zlang.symbols;

import java.util.Objects;

public final class Types {
    private Types() { }

    public static boolean isAssignable(Type lvalue, Type rvalue) {
        if (Objects.equals(lvalue, rvalue)) {
            return true;
        }
        if (lvalue == AnyType.INSTANCE) {
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
        if (rvalue instanceof AggregateTypeSymbol aggregate) {
            return aggregate.implementedInterfaces().contains(lvalue);
        }
        return false;
    }

    public static boolean isComparable(Type left, Type right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        if (left == BuiltInType.INT.type() && right == BuiltInType.BYTE.type()) {
            return true;
        }
        if (right == BuiltInType.INT.type() && left == BuiltInType.BYTE.type()) {
            return true;
        }
        if (left.registerType() == BuiltInType.OBJECT.type() && right == NilType.INSTANCE) {
            return true;
        }
        if (right.registerType() == BuiltInType.OBJECT.type() && left == NilType.INSTANCE) {
            return true;
        }
        if (left.registerType() == BuiltInType.STRING.type() && right == NilType.INSTANCE) {
            return true;
        }
        return right.registerType() == BuiltInType.STRING.type() && left == NilType.INSTANCE;
    }

    public static boolean isImplicitlyConvertible(Type target, Type source) {
        if (Objects.equals(target, source)) {
            return true;
        }
        if (target == AnyType.INSTANCE) {
            return true;
        }
        if (target == BuiltInType.STRING.type()) {
            return source instanceof ArrayType a && a.elementType() == BuiltInType.BYTE.type();
        }
        if (source == BuiltInType.STRING.type()) {
            return target instanceof ArrayType a && a.elementType() == BuiltInType.BYTE.type();
        }
        if (target == BuiltInType.INT.type()) {
            return source == BuiltInType.BYTE.type();
        }
        return source.registerType().id() == RegisterTypeId.Ref && target.registerType().id() == RegisterTypeId.Ref;
    }

    public static boolean isEffectivelyInteger(Type type) {
        return Types.isImplicitlyConvertible(BuiltInType.INT.type(), type);
    }

    public static Type promote(Type left, Type right) {
        if (isImplicitlyConvertible(left, right)) {
            return left;
        }
        if (isImplicitlyConvertible(right, left)) {
            return right;
        }
        return null;
    }
}
