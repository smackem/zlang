package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.ByteCodeWriter;
import net.smackem.zlang.emit.ir.Emitter;
import net.smackem.zlang.emit.ir.Instructions;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.SymbolExtractor;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InterpreterTest {

    @Test
    public void test() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var number: int = 0
                fn main() {
                    let x: int = 12
                    let y: int = 23
                    number = x + y
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, 16 * 1024);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(zl.isDirect()).isTrue();
        assertThat(zl.capacity()).isGreaterThan(16 * 1024);
        final int ignored = Interpreter.run(zl, program);
    }
}