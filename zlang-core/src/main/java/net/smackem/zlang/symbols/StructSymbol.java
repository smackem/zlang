package net.smackem.zlang.symbols;

public class StructSymbol extends AggregateTypeSymbol {
    StructSymbol(String name, Scope enclosingScope) {
        super(name, enclosingScope);
    }

    @Override
    public String toString() {
        return "StructSymbol{" +
               "name=" + name() +
               ", symbolTable=" + symbolTableString() +
               '}';
    }

    @Override
    public int byteSize() {
        int size = 0;
        for (final Symbol symbol : symbols()) {
            if (symbol instanceof FieldSymbol) {
                size += symbol.type().byteSize();
            }
        }
        return size;
    }
}
