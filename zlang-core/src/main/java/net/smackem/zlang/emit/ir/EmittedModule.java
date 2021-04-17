package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.Type;

import java.util.*;

final class EmittedModule {
    private final List<Type> types;
    private final List<FunctionSymbol> functions;
    private final List<Instruction> instructions;
    private final Map<FunctionSymbol, Instruction> codeMap;
    private final Collection<Label> labels;

    EmittedModule(List<Type> types,
                  List<FunctionSymbol> functions,
                  List<Instruction> instructions,
                  Map<FunctionSymbol, Instruction> codeMap,
                  Collection<Label> labels) {
        this.types = types;
        this.functions = functions;
        this.instructions = instructions;
        this.codeMap = codeMap;
        this.labels = labels;
    }

    public List<Type> types() {
        return types;
    }

    List<Instruction> instructions() {
        return instructions;
    }

    public Optional<Instruction> firstInstruction() {
        return this.instructions.isEmpty()
                ? Optional.empty()
                : Optional.of(this.instructions.get(0));
    }

    public Map<FunctionSymbol, Instruction> codeMap() {
        return codeMap;
    }

    public Collection<Label> labels() {
        return labels;
    }

    @Override
    public String toString() {
        return "EmittedModule[" +
               "types=" + types + ", " +
               "functions=" + functions + ", " +
               "instructions=" + instructions + ", " +
               "codeMap=" + codeMap + ", " +
               "labels=" + labels + ']';
    }
}
