package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import net.smackem.zlang.modules.SourceFileLocation;
import net.smackem.zlang.modules.SourceFileLocations;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class BasicTest {

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
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("number")).isEqualTo(12 + 23);
    }

    @Test
    public void panic() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var number: int = 123
                fn main() {
                    panic 0
                    number = 666
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("number")).isEqualTo(123);
    }

    @Test
    public void expressions() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var x1: int
                var x2: float
                var x3: byte
                var x4: bool
                var x5: bool
                var x6: bool
                var x7: bool
                fn main() {
                    x1 = 2 * 10 / 2 - 5 + 1
                    x2 = 100.0 * 5.0 / (2.0 + 8.0 - 5.0)
                    x3 = (byte) 1 + (byte) 2
                    x4 = 2 > 1 and 1 < 2 and 3 >= 3 and 3 <= 3 and 4 != 5 and 10 == 10
                    x5 = 2 > 1 and 1 < 2 and 3 >= 3 and 3 <= 3 and 4 != 5 and 10 == 11
                    x6 = 2 > 2 or 1 < 2 and 0 < 1
                    x7 = 2 == 2 and (1 < 1 + 2 or 1 > 1 - 1)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("x1")).isEqualTo(2 * 10 / 2 - 5 + 1);
        assertThat(globals.get("x2")).isEqualTo(100.0 * 5.0 / (2.0 + 8.0 - 5.0));
        assertThat(globals.get("x3")).isEqualTo((byte) 3);
        assertThat(globals.get("x4")).isEqualTo(true);
        assertThat(globals.get("x5")).isEqualTo(false);
        assertThat(globals.get("x6")).isEqualTo(true);
        assertThat(globals.get("x7")).isEqualTo(true);
    }

    @Test
    public void ternaryExpr() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                var numberB: int
                fn main() {
                    let one: int = 1
                    let two: int = 2
                    let zero: int = 0
                    let val1: int = 123
                    numberA = val1 if one < two else zero
                    numberB = zero if one > two else 321
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(123);
        assertThat(globals.get("numberB")).isEqualTo(321);
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
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("f1")).isEqualTo(12.5);
        assertThat(globals.get("f2")).isEqualTo(13.5);
        assertThat(globals.get("n")).isEqualTo(12);
        assertThat(globals.get("b")).isEqualTo((byte) 255);
        assertThat(globals.get("l")).isEqualTo(true);
    }

    @Test
    public void multiModule() throws Exception {
        final String mainSource = """
                module entry uses dep
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
                "entry", mainSource,
                "dep", depSource));
        final ParsedModule module = ParsedModule.parse("entry", loc);
        final Map<String, Object> globals = run(module.flatten());
        assertThat(globals.get("mainX")).isEqualTo(123);
        assertThat(globals.get("depY")).isEqualTo(234);
        assertThat(globals.get("depYCopy")).isEqualTo(234);
    }

    @Test
    public void runtimeTypeChecks() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface MyInterface {
                }
                struct MyStruct {
                    f: int
                } is MyInterface
                var result: bool[]
                fn main() {
                    let obj: object = new MyStruct{}
                    result = new bool[] {
                        obj is MyStruct,
                        obj is MyInterface,
                        nil is MyStruct,
                        obj is int list
                    }
                }
                """);
        InterpreterTests.writeZap(modules, Paths.get(System.getProperty("user.home"), "typeChecks.zap"));
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(new int[] { 1, 1, 0, 0 });
    }

    @Test
    public void nestedBlockExpr() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                var numberB: int
                fn main() {
                    numberA = {
                        yield 100
                    }
                    numberB = {
                        let x: int = {
                            let y: int = 80 + 20
                            yield y
                        }
                        yield x + 50
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(100);
        assertThat(globals.get("numberB")).isEqualTo(150);
    }

    @Test
    public void blockExprWithMultipleYields() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                var numberB: int
                fn main() {
                    let v: int = 100
                    numberA = {
                        if v > 666 {
                            yield v / 2
                        }
                        yield v
                    }
                    numberB = {
                        if v < 666 {
                            yield v
                        }
                        yield v * 2
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(100);
        assertThat(globals.get("numberB")).isEqualTo(200);
    }

    @Test
    public void blockExprWithIncompatibleYields() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                fn main() {
                    let v: int = 100
                    numberA = {
                        if v > 666 {
                            yield "a"
                        }
                        yield v
                    }
                }
                """);
        assertThatThrownBy(() -> run(modules)).hasMessageContaining("incompatible result types");
    }

    @Test
    public void advancedOperators() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var result: int[]
                fn main() {
                    result = new int[] {
                        10 % 9,
                        1 << 3,
                        8 >> 3,
                        10 & 3,
                        10 | 3,
                        10 ^ 123,
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(new int[] {
                10 % 9,
                1 << 3,
                8 >> 3,
                10 & 3,
                10 | 3,
                10 ^ 123,
        });
    }

    @Test
    public void unaryOperators() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let numberA: int = -100
                let numberB: float = -123.5
                let boolA: bool = not true
                let boolB: bool = not false
                fn main() {
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(-100);
        assertThat(globals.get("numberB")).isEqualTo(-123.5);
        assertThat(globals.get("boolA")).isEqualTo(false);
        assertThat(globals.get("boolB")).isEqualTo(true);
    }

    @Test
    public void hexNumbers() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let numberA: byte = 0xff
                let numberB: int = 0xff_ff
                let numberC: int = -0xff
                fn main() {
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo((byte) 0xff);
        assertThat(globals.get("numberB")).isEqualTo(0xffff);
        assertThat(globals.get("numberC")).isEqualTo(-0xff);
    }
}
