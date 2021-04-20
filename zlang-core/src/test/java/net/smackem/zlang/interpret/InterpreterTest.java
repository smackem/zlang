package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.ByteCodeWriter;
import net.smackem.zlang.emit.ir.Emitter;
import net.smackem.zlang.emit.ir.Instructions;
import net.smackem.zlang.emit.ir.Program;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import net.smackem.zlang.modules.SourceFileLocation;
import net.smackem.zlang.modules.SourceFileLocations;
import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.SymbolExtractor;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class InterpreterTest {
    private static final int defaultHeapSize = 1024;

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
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        assertThat(zl.isDirect()).isTrue();
        assertThat(zl.capacity()).isGreaterThan(defaultHeapSize);
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("number")).isEqualTo(12 + 23);
    }

    @Test
    public void initGlobals() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let f1: float = 12.5
                let f2: float = 13.5
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
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("f1")).isEqualTo(12.5);
        assertThat(globals.get("f2")).isEqualTo(13.5);
        assertThat(globals.get("n")).isEqualTo(12);
        assertThat(globals.get("b")).isEqualTo((byte) 255);
        assertThat(globals.get("l")).isEqualTo(true);
    }

    @Test
    public void simpleArray() throws Exception {
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
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("a1")).isEqualTo(1);
        assertThat(globals.get("a2")).isEqualTo(2);
        assertThat(globals.get("a3")).isEqualTo(3);
    }

    @Test
    public void moreArrays() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let ints: int[] = new int[2]
                let floats: float[] = new float[2]
                let bytes: byte[] = new byte[2]
                let arrays: bool[][] = new bool[][2]
                var f0: float
                var i0: int
                var a0: bool[]
                var a1: bool[]

                fn main() {
                    ints[0] = 1
                    ints[1] = 2
                    i0 = ints[0]
                    floats[0] = 12.5
                    floats[1] = 13.5
                    f0 = floats[0]
                    bytes[0] = (byte) 10
                    bytes[1] = (byte) 11
                    arrays[0] = new bool[2]
                    arrays[0][0] = true
                    arrays[0][1] = false
                    arrays[1] = new bool[2]
                    arrays[1][0] = false
                    arrays[1][1] = true
                    a0 = arrays[0]
                    a1 = arrays[1]
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("i0")).isEqualTo(1);
        assertThat(globals.get("ints")).isEqualTo(new int[] { 1, 2 });
        assertThat(globals.get("f0")).isEqualTo(12.5);
        assertThat(globals.get("floats")).isEqualTo(new double[] { 12.5, 13.5 });
        assertThat(globals.get("bytes")).isEqualTo(new byte[] { 10, 11 });
        assertThat(globals.get("a0")).isEqualTo(new int[] { 1, 0 });
        assertThat(globals.get("a1")).isEqualTo(new int[] { 0, 1 });
    }

    @Test
    public void simpleStruct() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct Sample {
                    index: int
                    value: float
                }
                let a: Sample = new Sample {
                    index: 1
                    value: 12.5
                }
                var index: int
                var value: float
                fn main() {
                    index = a.index
                    value = a.value
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("index")).isEqualTo(1);
        assertThat(globals.get("value")).isEqualTo(12.5);
        assertThat(globals.get("a")).isEqualTo(Map.of("index", 1, "value", 12.5));
    }

    @Test
    public void nestedStruct() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct SubSample {
                    id: byte
                    flag: bool
                }
                struct Sample {
                    index: int
                    value: float
                    sub: SubSample
                }
                let sample: Sample = new Sample {
                    index: 123
                    value: 12.5
                    sub: new SubSample {
                        id: (byte) 234
                    }
                }
                fn main() {
                    sample.sub.flag = true
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("sample")).isEqualTo(Map.of(
                "index", 123,
                "value", 12.5,
                "sub", Map.of(
                        "id", (byte) 234,
                        "flag", true
                )
        ));
    }

    @Test
    public void arraysOfStructs() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct Sample {
                    id: int
                    flag: bool
                    data: float[]
                }
                let samples: mutable Sample[] = new Sample[2]
                var firstSample: Sample
                fn main() {
                    samples[0] = new Sample {
                        id: 123
                        data: new float[2]
                    }
                    samples[0].data[0] = 12.5
                    samples[0].data[1] = 13.5
                    firstSample = samples[0]
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        //noinspection unchecked
        assertThat((Map<String, Object>) globals.get("firstSample")).containsOnly(
                Map.entry("id", 123),
                Map.entry("flag", false),
                Map.entry("data", new double[] { 12.5, 13.5 }));
    }

    @Test
    public void nil() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let x: object = nil
                var y: int[]
                struct NilContainer {
                    m: object
                    n: float[]
                }
                var nilContainer: NilContainer
                fn main() {
                    nilContainer = new NilContainer {}
                }
                """);
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("x")).isNull();
        assertThat(globals.get("y")).isNull();
        //noinspection unchecked
        assertThat(((Map<String, Object>) globals.get("nilContainer")).get("m")).isNull();
        //noinspection unchecked
        assertThat(((Map<String, Object>) globals.get("nilContainer")).get("n")).isNull();
    }

    @Test
    public void multiModule() throws Exception {
        final String mainSource = """
                module main uses dep
                let mainX: int = 123
                var depYCopy: int
                fn main() {
                    depYCopy = depY
                }
                """;
        final String depSource = """
                let depY: int = 234
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep", depSource));
        final ParsedModule module = ParsedModule.parse("main", loc);
        final Collection<ParsedModule> modules = module.flatten();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("mainX")).isEqualTo(123);
        assertThat(globals.get("depY")).isEqualTo(234);
        assertThat(globals.get("depYCopy")).isEqualTo(234);
    }

    @Test
    public void multiModuleTypes() throws Exception {
        final String mainSource = """
                module main uses dep1
                var dep1: Dep1Type = new Dep1Type {}
                var dep2: Dep2Type = new Dep2Type {}
                fn main() {
                    dep1.f = 123
                    dep2.f = 234
                }
                """;
        final String dep1Source = """
                module dep1 uses dep2
                struct Dep1Type {
                    f: int
                }
                """;
        final String dep2Source = """
                struct Dep2Type {
                    f: int
                }
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep1", dep1Source,
                "dep2", dep2Source));
        final ParsedModule module = ParsedModule.parse("main", loc);
        final Collection<ParsedModule> modules = module.flatten();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, new GlobalScope(), errors);
        final Program program = Emitter.emit(ps, modules);
        final ByteCodeWriter writer = new ByteCodeWriter();
        final ByteBuffer zl = writer.writeProgram(program, defaultHeapSize);
        System.out.println(Instructions.print(program.instructions()));
        assertThat(errors).isEmpty();
        final Map<String, Object> globals = Interpreter.run(zl, program);
        assertThat(globals.get("dep1")).isEqualTo(Map.of("f", 123));
        assertThat(globals.get("dep2")).isEqualTo(Map.of("f", 234));
    }
}
