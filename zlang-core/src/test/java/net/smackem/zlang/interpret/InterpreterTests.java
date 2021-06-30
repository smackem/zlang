package net.smackem.zlang.interpret;

import net.smackem.zlang.compiler.ZLCompiler;
import net.smackem.zlang.emit.bytecode.ByteCodeReader;
import net.smackem.zlang.emit.bytecode.ByteCodeWriter;
import net.smackem.zlang.emit.bytecode.ByteCodeWriterOptions;
import net.smackem.zlang.emit.bytecode.HeapEntry;
import net.smackem.zlang.emit.ir.Emitter;
import net.smackem.zlang.emit.ir.Instructions;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.SymbolExtractor;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class InterpreterTests {
    public static final int HEAP_SIZE = 4096;
    private static final int maxStackDepth = 16;

    private InterpreterTests() { }

    static Map<String, Object> run(Collection<ParsedModule> modules) throws Exception {
        final ZLCompiler.CompilationResult result = compile(modules);
        return Interpreter.run(result.firstZap(), result.program());
    }

    static Collection<HeapEntry> runExtractingHeap(Collection<ParsedModule> modules) throws Exception {
        final ZLCompiler.CompilationResult result = compile(modules);
        final int heapOffset = Interpreter.run(result.firstZap());
        return ByteCodeReader.readHeap(result.firstZap(), heapOffset);
    }

    static void writeZap(Collection<ParsedModule> modules, Path path) throws Exception {
        final ByteCodeWriterOptions options = new ByteCodeWriterOptions()
                .isMemoryImage(false)
                .hasHeapSizeLimit(true)
                .heapSize(HEAP_SIZE)
                .maxStackDepth(maxStackDepth);
        final ZLCompiler.CompilationResult result = compile(modules, options);
        try (final OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            os.write(result.firstZap().array(), result.firstZap().arrayOffset(), result.firstZap().limit());
        }
    }

    private static ZLCompiler.CompilationResult compile(Collection<ParsedModule> modules) throws Exception {
        final ByteCodeWriterOptions options = new ByteCodeWriterOptions()
                .isMemoryImage(true)
                .heapSize(HEAP_SIZE)
                .hasHeapSizeLimit(true)
                .maxStackDepth(maxStackDepth);
        return compile(modules, options);
    }

    private static ZLCompiler.CompilationResult compile(Collection<ParsedModule> modules, ByteCodeWriterOptions options) throws Exception {
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        assertThat(errors).isEmpty();
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zap = writer.writeProgram(program, options);
        assertThat(zap.isDirect()).isEqualTo(options.isMemoryImage());
        if (options.isMemoryImage()) {
            assertThat(zap.capacity()).isGreaterThan(HEAP_SIZE);
        }
        System.out.println(Instructions.print(program.instructions()));
        return new ZLCompiler.CompilationResult(program, List.of(zap));
    }
}
