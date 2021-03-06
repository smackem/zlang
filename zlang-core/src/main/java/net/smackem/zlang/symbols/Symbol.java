package net.smackem.zlang.symbols;

import java.util.Objects;

public abstract class Symbol {
    private final String name;
    private final Type type;
    private int address;
    private int line;
    private int position;
    private Scope definingScope;

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

    public int address() {
        return this.address;
    }

    public void setAddress(int address) {
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

    public Scope definingScope() {
        return this.definingScope;
    }

    void setDefiningScope(Scope scope) {
        this.definingScope = scope;
    }

    @Override
    public String toString() {
        return "Symbol{" +
               "name='" + name + '\'' +
               ", type=" + type +
               '}';
    }
}
