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
}
