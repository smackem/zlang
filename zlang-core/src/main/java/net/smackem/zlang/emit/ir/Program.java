package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.Type;
import net.smackem.zlang.symbols.VariableSymbol;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Program {
    private final List<Instruction> instructions;
    private final Collection<EmittedModule> modules;
    private final Collection<VariableSymbol> globals;
    private final Collection<Type> emittedTypes;
    private Map<FunctionSymbol, FunctionCode> cachedCodeMap;

    private Program(List<Instruction> instructions,
                    Collection<EmittedModule> modules,
                    Collection<VariableSymbol> globals,
                    Collection<Type> emittedTypes) {
        this.instructions = instructions;
        this.modules = modules;
        this.globals = globals;
        this.emittedTypes = emittedTypes;
    }

    static Program emit(Collection<EmittedModule> modules,
                        Collection<VariableSymbol> globals,
                        Collection<Type> emittedTypes) {
        final List<Instruction> instructions = new ArrayList<>();
        for (final EmittedModule em : modules) {
            instructions.addAll(em.instructions());
        }
        return new Program(instructions, modules, globals, emittedTypes);
    }

    Program freeze() {
        return new Program(Collections.unmodifiableList(this.instructions),
                Collections.unmodifiableCollection(this.modules),
                Collections.unmodifiableCollection(this.globals),
                Collections.unmodifiableCollection(this.emittedTypes));
    }

    public List<Instruction> instructions() {
        return instructions;
    }

    public Collection<Type> types() {
        return Stream.concat(this.modules.stream().flatMap(m -> m.types().stream()),
                this.emittedTypes.stream())
                .toList();
    }

    public Collection<VariableSymbol> globals() {
        return this.globals;
    }

    public int globalSegmentSize() {
        return this.globals.stream()
                .mapToInt(s -> s.type().byteSize())
                .sum();
    }

    public Map<FunctionSymbol, FunctionCode> codeMap() {
        if (this.cachedCodeMap == null) {
            this.cachedCodeMap = this.modules.stream()
                    .flatMap(em -> em.codeMap().entrySet().stream()
                            .map(entry -> Map.entry(entry.getKey(),
                                    new FunctionCode(entry.getValue(), em.firstInstruction().orElseThrow()))))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return this.cachedCodeMap;
    }

    public FunctionSymbol entryPoint() {
        return this.modules.stream()
                .flatMap(em -> em.codeMap().keySet().stream())
                .filter(f -> Objects.equals(f.name(), "main"))
                .findFirst()
                .orElseThrow();
    }

    public Collection<Label> labels() {
        return this.modules.stream()
                .flatMap(f -> f.labels().stream())
                .toList();
    }
}
