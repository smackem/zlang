package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.FunctionSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EmittedFunction {
    private final FunctionSymbol symbol;
    final List<Instruction> instructions = new ArrayList<>();

    EmittedFunction(FunctionSymbol symbol) {
        this.symbol = Objects.requireNonNull(symbol);
    }

    public FunctionSymbol symbol() {
        return this.symbol;
    }

    public int parameterCount() {
        return this.symbol.symbols().size();
    }
}
