package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.ByteCodeWriter;
import net.smackem.zlang.emit.ir.Emitter;
import net.smackem.zlang.emit.ir.Instructions;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.SymbolExtractor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InterpreterTests {
    private static final int heapSize = 1024;
    private static final int maxStackDepth = 16;

    private InterpreterTests() { }

    static Map<String, Object> run(Collection<ParsedModule> modules) throws Exception {
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, heapSize, maxStackDepth);
        assertThat(zl.isDirect()).isTrue();
        assertThat(zl.capacity()).isGreaterThan(heapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        return Interpreter.run(zl, program);
    }
}
