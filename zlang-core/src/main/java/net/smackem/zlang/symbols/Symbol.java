package net.smackem.zlang.symbols;

import java.util.Objects;

public abstract class Symbol {
    private final String name;
    private final Type type;
    private long address;
    private int line;
    private int position;

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

    public long address() {
        return this.address;
    }

    public void setAddress(long address) {
        this.address = address;
    }

    public int lineNumber() {
        return this.line;
    }

    void setLineNumber(int line) {
        this.line = line;
    }

    public int charPosition() {
        return this.position;
    }

    void setCharPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Symbol{" +
               "name='" + name + '\'' +
               ", type=" + type +
               '}';
    }
}
