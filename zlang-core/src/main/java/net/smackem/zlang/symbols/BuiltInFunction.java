package net.smackem.zlang.symbols;

public enum BuiltInFunction {
    ARRAY_LENGTH("length", 1),
    ARRAY_COPY("copy", 2);

    private final String ident;
    private final int address;

    BuiltInFunction(String ident, int address) {
        this.ident = ident;
        this.address = address;
    }

    public String ident() {
        return this.ident;
    }

    public int address() {
        return this.address;
    }
}
