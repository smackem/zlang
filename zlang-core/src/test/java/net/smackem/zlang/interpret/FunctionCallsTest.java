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

public class FunctionCallsTest {

    @Test
    public void voidFunctionCall() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var x: int
                fn func() {
                    x = 123
                }
                fn main() {
                    func()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("x")).isEqualTo(123);
    }

    @Test
    public void functionCallWithReturnValue() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var x: int
                fn func() -> int {
                    return 123
                }
                fn main() {
                    x = func()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("x")).isEqualTo(123);
    }

    @Test
    public void functionCallWithArguments() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var x: float = 1.0
                fn addAll(a: float, b: int, c: byte, d: float, e: int, f: byte) -> float {
                    return x + a + (float) b + (float) c + d + (float) e + (float) f
                }
                fn main() {
                    let five: byte = (byte) 5
                    let four: int = 4
                    let threePointFive: float = 3.5
                    x = addAll(1.5, 2, (byte) 3, threePointFive, four, five)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("x")).isEqualTo(20.0);
    }

    @Test
    public void nestedFunctionCalls() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var a: int
                var b: int
                var c: int
                var r: int
                fn do_a(x: int) -> int {
                    a = x
                    return do_b(x + 1)
                }
                fn do_b(x: int) -> int {
                    b = x
                    return do_c(x + 1)
                }
                fn do_c(x: int) -> int {
                    c = x
                    return x
                }
                fn main() {
                    r = do_a(1)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("a")).isEqualTo(1);
        assertThat(globals.get("b")).isEqualTo(2);
        assertThat(globals.get("c")).isEqualTo(3);
        assertThat(globals.get("r")).isEqualTo(3);
    }

    @Test
    public void recursiveFunctionCalls() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var a: int

                fn recurse(x: int) -> int {
                    if x >= 10 {
                        return 10
                    }
                    return x + recurse(x + 1)
                }

                fn main() {
                    a = recurse(1)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("a")).isEqualTo(55);
    }

    @Test
    public void voidMethodCall() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct MyStruct {
                    x: int
                }
                fn MyStruct::func() {
                    self.x = 123
                }
                let r: MyStruct = new MyStruct{}
                fn main() {
                    r.func()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("r")).isEqualTo(Map.of(
                "x", 123));
    }

    @Test
    public void methodCallWithReturnValue() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct MyStruct {
                    x: int
                }
                fn MyStruct::func() -> float {
                    return (float) self.x + 1.0
                }
                let r: MyStruct = new MyStruct{
                    x: 123
                }
                fn main() {
                    r.x = (int) r.func()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("r")).isEqualTo(Map.of(
                "x", 124));
    }

    @Test
    public void methodCallWithArguments() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct MyStruct {
                    x: float
                }
                fn MyStruct::addAll(a: float, b: int, c: byte, d: float, e: int, f: byte) -> float {
                    return self.x + a + (float) b + (float) c + d + (float) e + (float) f
                }
                let r: MyStruct = new MyStruct{
                    x: 1.0
                }
                var ret: int
                fn main() {
                    r.x = r.addAll(1.5, 2, (byte) 3, 3.5, 4, (byte) 5)
                    ret = (int) r.x
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("ret")).isEqualTo(20);
    }

    @Test
    public void nestedMethodCalls() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct MyStruct {
                    a: int
                    b: int
                    c: int
                }
                fn MyStruct::do_a(x: int) -> int {
                    self.a = x
                    return self.do_b(x + 1)
                }
                fn MyStruct::do_b(x: int) -> int {
                    self.b = x
                    return self.do_c(x + 1)
                }
                fn MyStruct::do_c(x: int) -> int {
                    self.c = x
                    return x + 1
                }
                let r: MyStruct = new MyStruct{}
                var ret: int
                fn main() {
                    ret = r.do_a(1)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("r")).isEqualTo(Map.of(
                "a", 1,
                "b", 2,
                "c", 3));
        assertThat(globals.get("ret")).isEqualTo(4);
    }

    @Test
    public void nestedMethodCallsMultipleTypes() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct StructA {
                    a: int
                }
                struct StructB {
                    b: int
                }
                struct StructC {
                    c: int
                }
                fn StructA::doIt(x: int, b: StructB, c: StructC) -> int {
                    self.a = x
                    return b.doIt(x + 1, c)
                }
                fn StructB::doIt(x: int, c: StructC) -> int {
                    self.b = x
                    return c.doIt(x + 1)
                }
                fn StructC::doIt(x: int) -> int {
                    self.c = x
                    return x + 1
                }
                let sa: StructA = new StructA{}
                let sb: StructB = new StructB{}
                let sc: StructC = new StructC{}
                var ret: int
                fn main() {
                    ret = sa.doIt(1, sb, sc)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("sa")).isEqualTo(Map.of("a", 1));
        assertThat(globals.get("sb")).isEqualTo(Map.of("b", 2));
        assertThat(globals.get("sc")).isEqualTo(Map.of("c", 3));
        assertThat(globals.get("ret")).isEqualTo(4);
    }

    @Test
    public void multiModuleMethods() throws Exception {
        final String mainSource = """
                module main uses dep1
                var dep1: Dep1Type = new Dep1Type {}
                var dep2: Dep2Type = new Dep2Type {}
                var r: int
                fn main() {
                    r = dep2.func(dep1.func(1))
                }
                """;
        final String dep1Source = """
                module dep1 uses dep2
                struct Dep1Type {
                    f: int
                }
                fn Dep1Type::func(x: int) -> int {
                    self.f = x
                    return x + 1
                }
                """;
        final String dep2Source = """
                struct Dep2Type {
                    f: int
                }
                fn Dep2Type::func(x: int) -> int {
                    self.f = x
                    return x + 1
                }
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep1", dep1Source,
                "dep2", dep2Source));
        final ParsedModule module = ParsedModule.parse("main", loc);
        final Map<String, Object> globals = run(module.flatten());
        assertThat(globals.get("dep1")).isEqualTo(Map.of("f", 1));
        assertThat(globals.get("dep2")).isEqualTo(Map.of("f", 2));
        assertThat(globals.get("r")).isEqualTo(3);
    }

    @Test
    public void recursiveFunctionCallWithTernary() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var a: int

                fn fib(x: int) -> int {
                    return 0 if x <= 0 else 1 if x <= 2 else fib(x - 2) + fib(x - 1)
                }

                fn main() {
                    a = fib(10)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("a")).isEqualTo(55);
    }
}
