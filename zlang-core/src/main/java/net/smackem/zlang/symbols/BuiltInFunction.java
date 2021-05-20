package net.smackem.zlang.symbols;

public enum BuiltInFunction {
    ARRAY_LENGTH("length", -1),
    ARRAY_COPY("copy", -2);

    private final String functionName;
    private final int address;

    BuiltInFunction(String functionName, int address) {
        this.functionName = functionName;
        this.address = address;
    }

    public String functionName() {
        return this.functionName;
    }

    public int address() {
        return this.address;
    }
}
