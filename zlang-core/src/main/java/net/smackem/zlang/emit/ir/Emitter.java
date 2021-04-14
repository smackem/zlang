package net.smackem.zlang.emit.ir;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.Type;

import java.util.*;

public final class Emitter {

    private Emitter() { }

    public static Program emit(ProgramStructure ps, List<ParsedModule> parsedModules) {
        final List<EmittedModule> ems = new ArrayList<>();
        for (final ParsedModule module : parsedModules) {
            final EmitWalker emitWalker = new EmitWalker(module.moduleName(), ps);
            module.ast().accept(emitWalker);
            ems.add(emitWalker.buildModule());
        }
        return buildProgram(ems);
    }

    private static Program buildProgram(Collection<EmittedModule> emittedModules) {
        final List<Instruction> instructions = new ArrayList<>();
        final List<Type> types = new ArrayList<>();
        final Map<FunctionSymbol, Instruction> codeMap = new HashMap<>();
        FunctionSymbol entryPoint = null;
        Instruction entryPointBaseInstruction = null;
        for (final EmittedModule em : emittedModules) {
            final Optional<FunctionSymbol> ep = em.functions().stream()
                    .filter(f -> Objects.equals(f.name(), "main"))
                    .findFirst();
            if (ep.isPresent()) {
                entryPoint = ep.get();
                entryPointBaseInstruction = em.instructions().get(0); // if the module has an entry point, it also has instructions
            }
            instructions.addAll(em.instructions());
            types.addAll(em.types());
            for (final var entry : em.codeMap().entrySet()) {
                codeMap.put(entry.getKey(), entry.getValue());
            }
        }
        return new Program(instructions, types, codeMap, entryPoint, entryPointBaseInstruction);
    }
}
