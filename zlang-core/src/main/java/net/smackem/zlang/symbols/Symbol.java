package net.smackem.zlang.symbols;

public abstract class Symbol {
    private final String name;
    private final Type type;

    Symbol(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    public String name() {
        return this.name;
    }

    public Type type() {
        return this.type;
    }
}
