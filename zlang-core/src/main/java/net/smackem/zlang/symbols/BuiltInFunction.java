package net.smackem.zlang.symbols;

public enum BuiltInFunction {
    ARRAY_SIZE("size", 1),
    ARRAY_COPY("copy", 2),
    ARRAY_WRAP("wrap", 3),
    LIST_SIZE("size", 10),
    LIST_CAPACITY("capacity", 11),
    LIST_ADD("add", 12),
    LIST_REMOVE("remove", 13),
    PRINT("print", 20),
    STRING_LENGTH("length", 30);

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
