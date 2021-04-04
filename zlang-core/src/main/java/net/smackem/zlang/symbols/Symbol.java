package net.smackem.zlang.symbols;

import java.util.Objects;

public abstract class Symbol {
    private final String name;
    private final Type type;

    Symbol(String name, Type type) {
        this.name = Objects.requireNonNull(name);
        this.type = type;
    }

    public String name() {
        return this.name;
    }

    public Type type() {
        return this.type;
    }

    @Override
    public String toString() {
        return "Symbol{" +
               "name='" + name + '\'' +
               ", type=" + type +
               '}';
    }
}
