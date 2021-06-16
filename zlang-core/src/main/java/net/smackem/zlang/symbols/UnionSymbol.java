package net.smackem.zlang.symbols;

import net.smackem.zlang.emit.ir.Naming;
import net.smackem.zlang.lang.CompilationErrorException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class UnionSymbol extends AggregateTypeSymbol {
    private final Map<String, Integer> fieldIds = new LinkedHashMap<>();

    UnionSymbol(String name, Scope enclosingScope) {
        super(name, enclosingScope);
    }

    public int getFieldId(Symbol field) {
        assert this.symbols().contains(field);
        return this.fieldIds.get(field.name());
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
        int flagFieldSize = 0;
        for (final Symbol symbol : symbols()) {
            if (Objects.equals(symbol.name(), Naming.UNION_FLAG_FIELD_NAME) == false) {
                size = Math.max(size, symbol.type().registerType().byteSize());
            } else {
                flagFieldSize = symbol.type().registerType().byteSize();
            }
        }
        return flagFieldSize + size;
    }

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        super.define(name, symbol);
        if (Objects.equals(name, Naming.UNION_FLAG_FIELD_NAME)) {
            return;
        }
        this.fieldIds.computeIfAbsent(name, ignored -> this.fieldIds.size());
    }
}
