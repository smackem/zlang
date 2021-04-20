package net.smackem.zlang.emit.ir;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.symbols.ProgramStructure;

import java.util.*;

public final class Emitter {

    private Emitter() { }

    public static Program emit(ProgramStructure ps, Collection<ParsedModule> parsedModules) {
        final List<EmittedModule> ems = new ArrayList<>();
        for (final ParsedModule module : parsedModules) {
            final EmitWalker emitWalker = new EmitWalker(module.moduleName(), ps);
            module.ast().accept(emitWalker);
            ems.add(emitWalker.buildModule());
        }
        return buildProgram(ems, ps);
    }

    private static Program buildProgram(Collection<EmittedModule> emittedModules, ProgramStructure ps) {
        final Program program = Program.emit(emittedModules, ps.globals());
        fixupEntryPoint(program);
        return program.freeze();
    }

    private static void fixupEntryPoint(Program program) {
        final FunctionCode fc = program.codeMap().get(program.entryPoint());
        final List<Instruction> instructions = program.instructions();
        final int index = instructions.indexOf(fc.firstInstr());
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
