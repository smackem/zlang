package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.ByteCodeReader;
import net.smackem.zlang.emit.bytecode.ByteCodeWriter;
import net.smackem.zlang.emit.bytecode.HeapEntry;
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
    public static final int HEAP_SIZE = 4096;
    private static final int maxStackDepth = 16;

    private InterpreterTests() { }

    static Map<String, Object> run(Collection<ParsedModule> modules) throws Exception {
        final CompilationResult result = compile(modules);
        return Interpreter.run(result.zl(), result.program());
    }

    static Collection<HeapEntry> runExtractingHeap(Collection<ParsedModule> modules) throws Exception {
        final CompilationResult result = compile(modules);
        final int heapOffset = Interpreter.run(result.zl());
        return ByteCodeReader.readHeap(result.zl(), heapOffset);
    }

    private static record CompilationResult(Program program, ByteBuffer zl) {}

    private static CompilationResult compile(Collection<ParsedModule> modules) throws Exception {
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, HEAP_SIZE, true, maxStackDepth);
        assertThat(zl.isDirect()).isTrue();
        assertThat(zl.capacity()).isGreaterThan(HEAP_SIZE);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        return new CompilationResult(program, zl);
    }
}
