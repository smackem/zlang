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
        return buildProgram(ems, ps);
    }

    private static Program buildProgram(Collection<EmittedModule> emittedModules, ProgramStructure ps) {
        final List<Instruction> instructions = new ArrayList<>();
        final List<Type> types = new ArrayList<>();
        final Map<FunctionSymbol, Instruction> codeMap = new HashMap<>();
        final List<Label> labels = new ArrayList<>();
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
            labels.addAll(em.labels());
        }
        final Program program = new Program(instructions, types, ps.globals(), codeMap, entryPoint, entryPointBaseInstruction, labels);
        fixupEntryPoint(program);
        return program.freeze();
    }

    private static void fixupEntryPoint(Program program) {
        final Instruction instr = program.codeMap().get(program.entryPoint());
        final List<Instruction> instructions = program.instructions();
        final int index = instructions.indexOf(instr);
        program.codeMap().keySet().stream()
                .filter(f -> f.name().startsWith(Naming.GENERATED_INIT_FUNCTION_PREFIX))
                .forEach(f -> {
                    final Instruction call = new Instruction(OpCode.Call);
                    call.setRegisterArg(0, Register.R000);
                    call.setRegisterArg(1, Register.R000);
                    call.setSymbolArg(f);
                    instructions.add(index + 1, call);
                });
    }
}
