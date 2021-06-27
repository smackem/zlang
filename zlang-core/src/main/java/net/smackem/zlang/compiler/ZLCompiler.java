package net.smackem.zlang.compiler;

import com.google.common.base.Joiner;
import net.smackem.zlang.emit.bytecode.ByteCodeWriter;
import net.smackem.zlang.emit.bytecode.ByteCodeWriterOptions;
import net.smackem.zlang.emit.ir.Emitter;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.SourceFileLocation;
import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.Symbol;
import net.smackem.zlang.symbols.SymbolExtractor;
import org.antlr.v4.runtime.ParserRuleContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ZLCompiler {
    private ZLCompiler() { throw new AssertionError("no instance"); }

    public static record CompilationResult(Program program, ByteBuffer zap) {}

    public static CompilationResult compile(SourceFileLocation location, String moduleName, ByteCodeWriterOptions options) throws Exception {
        final ParsedModule module = ParsedModule.parse(moduleName, location);
        final Collection<ParsedModule> modules = module.flatten();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        if (errors.isEmpty() == false) {
            throw new CompilationErrorException(Joiner.on(System.lineSeparator()).join(errors));
        }
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zap = writer.writeProgram(program, options);
        assert zap.isDirect() == options.isMemoryImage();
        return new CompilationResult(program, zap);
    }
}
