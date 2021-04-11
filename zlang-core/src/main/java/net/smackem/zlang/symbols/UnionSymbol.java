package net.smackem.zlang.symbols;

public class UnionSymbol extends AggregateTypeSymbol {

    UnionSymbol(String name, Scope enclosingScope) {
        super(name, enclosingScope);
    }

    @Override
    public String toString() {
        return "UnionSymbol{" +
               "name=" + name() +
               ", symbolTable=" + symbolTableString() +
               '}';
    }

    @Override
    public int byteSize() {
        int size = 0;
        for (final Symbol symbol : symbols()) {
            size = Math.max(size, symbol.type().byteSize());
        }
        return size;
    }
}
