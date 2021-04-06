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
}
