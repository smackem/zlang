package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import net.smackem.zlang.modules.SourceFileLocation;
import net.smackem.zlang.modules.SourceFileLocations;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AggregateTypesTest {

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
        final Map<String, Object> globals = run(modules);
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
        final Map<String, Object> globals = run(modules);
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
        final Map<String, Object> globals = run(modules);
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
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("x")).isNull();
        assertThat(globals.get("y")).isNull();
        //noinspection unchecked
        assertThat(((Map<String, Object>) globals.get("nilContainer")).get("m")).isNull();
        //noinspection unchecked
        assertThat(((Map<String, Object>) globals.get("nilContainer")).get("n")).isNull();
    }

    @Test
    public void multiModuleTypes() throws Exception {
        final String mainSource = """
                module main uses dep1
                var dep1: Dep1Type = makeDep1Type()
                var dep2: Dep2Type = makeDep2Type()
                fn main() {
                    dep1.setF(123)
                    dep2.setF(234)
                }
                """;
        final String dep1Source = """
                module dep1 uses dep2
                let x1: int = 444
                struct Dep1Type {
                    f: int
                }
                fn makeDep1Type() -> Dep1Type {
                    return new Dep1Type {}
                }
                fn Dep1Type::setF(value: int) {
                    self.f = value
                }
                """;
        final String dep2Source = """
                struct Dep2Type {
                    f: int
                }
                fn makeDep2Type() -> Dep2Type {
                    return new Dep2Type {}
                }
                fn Dep2Type::setF(value: int) {
                    self.f = value
                }
                let x2: int = 555
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep1", dep1Source,
                "dep2", dep2Source));
        final ParsedModule module = ParsedModule.parse("main", loc);
        final Map<String, Object> globals = run(module.flatten());
        assertThat(globals.get("dep1")).isEqualTo(Map.of("f", 123));
        assertThat(globals.get("dep2")).isEqualTo(Map.of("f", 234));
    }

    @Test
    public void unionCreation() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                union Value {
                    n: int
                    f: float
                    s: string
                }
                let a: Value = new Value.n(123)
                let b: Value = new Value.f(123.25)
                let c: Value = new Value.s("abc")
                fn main() {
                }
                """);
        final Map<String, Object> globals = run(modules);
        //noinspection unchecked
        assertThat(((Map<String, Object>) globals.get("a")).get("n")).isEqualTo(123);
        //noinspection unchecked
        assertThat(((Map<String, Object>) globals.get("b")).get("f")).isEqualTo(123.25);
        //noinspection unchecked
        assertThat(((Map<String, Object>) globals.get("c")).get("s")).isEqualTo("abc");
    }

    @Test
    public void unionFieldAccessForbidden() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                union Value {
                    n: int
                    f: float
                    s: string
                }
                fn main() {
                    let v: Value = new Value.n(123)
                    let notAllowed: int = v.n
                }
                """);
        assertThatThrownBy(() -> run(modules)).hasMessageContaining("not a struct");
    }

    @Test
    public void unionCreationAsStructForbidden() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                union Value {
                    n: int
                    f: float
                    s: string
                }
                fn main() {
                    let v: Value = new Value {
                        n: 123
                        f: 444.5
                    }
                }
                """);
        assertThatThrownBy(() -> run(modules)).hasMessageContaining("not a struct");
    }
}
