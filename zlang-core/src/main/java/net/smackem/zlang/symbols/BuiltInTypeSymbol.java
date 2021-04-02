package net.smackem.zlang.symbols;

public class BuiltInTypeSymbol extends Symbol implements Type {

    public static final BuiltInTypeSymbol INT = new BuiltInTypeSymbol("int");
    public static final BuiltInTypeSymbol FLOAT = new BuiltInTypeSymbol("float");
    public static final BuiltInTypeSymbol BYTE = new BuiltInTypeSymbol("byte");
    public static final BuiltInTypeSymbol ANY = new BuiltInTypeSymbol("any");
    public static final BuiltInTypeSymbol RUNTIME_PTR = new BuiltInTypeSymbol("runtime_ptr");
    public static final BuiltInTypeSymbol STRING = new BuiltInTypeSymbol("string");

    private final String typeName;

    private BuiltInTypeSymbol(String typeName) {
        super(typeName, null);
        this.typeName = typeName;
    }

    @Override
    public String typeName() {
        return this.typeName;
    }
}
