package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.ByteCodeWriter;
import net.smackem.zlang.emit.ir.Emitter;
import net.smackem.zlang.emit.ir.Instructions;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.SymbolExtractor;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class InterpreterTest {

    @Test
    public void minimal() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var number: int
                fn main() {
                    let x: int = 12
                    let y: int = 23
                    number = x + y
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, 16 * 1024);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        assertThat(zl.isDirect()).isTrue();
        assertThat(zl.capacity()).isGreaterThan(16 * 1024);
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("number")).isEqualTo(12 + 23);
    }

    @Test
    public void initGlobals() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let f: float = 12.5
                let n: int = 12
                let b: byte = (byte) 255
                let l: bool = true
                fn main() {
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, 16 * 1024);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("f")).isEqualTo(12.5);
        assertThat(globals.get("n")).isEqualTo(12);
        assertThat(globals.get("b")).isEqualTo((byte) 255);
        assertThat(globals.get("l")).isEqualTo(true);
    }

    @Test
    public void arrays() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let a: int[] = new int[3]
                var a1: int
                var a2: int
                var a3: int
                fn main() {
                    a[0] = 1
                    a[1] = 2
                    a[2] = 3
                    a1 = a[0]
                    a2 = a[1]
                    a3 = a[2]
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, 16 * 1024);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("a1")).isEqualTo(1);
        assertThat(globals.get("a2")).isEqualTo(2);
        assertThat(globals.get("a3")).isEqualTo(3);
    }
}
