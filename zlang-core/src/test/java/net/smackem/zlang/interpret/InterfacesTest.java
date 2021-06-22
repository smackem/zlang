package net.smackem.zlang.interpret;

import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InterfacesTest {
    @Test
    public void dynamicDispatch() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn getNumber() -> int
                }

                struct NumberGetterImpl {
                    x: int
                } is NumberGetter

                fn NumberGetterImpl::getNumber() -> int {
                    return self.x
                }

                var result: int
                fn main() {
                    let obj: NumberGetter = new NumberGetterImpl {
                        x = 123
                    }
                    result = obj.getNumber()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void multipleInterfaceStaticDispatch() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn getNumber() -> int
                }
                interface NumberSetter {
                    fn setNumber(n: int) -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter, NumberSetter

                fn NumberImpl::getNumber() -> int {
                    return self.x
                }

                fn NumberImpl::setNumber(n: int) -> int {
                    self.x = n
                    return n
                }

                var result: int
                fn main() {
                    let obj: NumberImpl = new NumberImpl {
                        x = 0
                    }
                    obj.setNumber(123)
                    result = obj.getNumber()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void dynamicDispatchParameter() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn getNumber() -> int
                }

                struct NumberGetterImpl {
                    x: int
                } is NumberGetter

                fn NumberGetterImpl::getNumber() -> int {
                    return self.x
                }
                
                fn getNumberFrom(getter: NumberGetter) -> int {
                    return getter.getNumber()
                }

                var result: int
                fn main() {
                    let obj: NumberGetterImpl = new NumberGetterImpl {
                        x = 123
                    }
                    result = getNumberFrom(obj)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void multipleInterfaceDynamicDispatch() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn getNumber() -> int
                }
                interface NumberSetter {
                    fn setNumber(n: int) -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter, NumberSetter

                fn NumberImpl::getNumber() -> int {
                    return self.x
                }

                fn NumberImpl::setNumber(n: int) -> int {
                    self.x = n
                    return n
                }

                var result: int
                fn main() {
                    let obj: NumberImpl = new NumberImpl {
                        x = 0
                    }
                    let getter: NumberGetter = obj
                    let setter: NumberSetter = obj
                    setter.setNumber(123)
                    result = getter.getNumber()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void multipleInterfaceDynamicDispatchDoubleImpl() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter1 {
                    fn getNumber() -> int
                }
                interface NumberGetter2 {
                    fn getNumber() -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter1, NumberGetter2

                fn NumberImpl::getNumber() -> int {
                    return self.x
                }

                var result: int
                fn main() {
                    let obj: NumberImpl = new NumberImpl {
                        x = 100
                    }
                    let getter1: NumberGetter1 = obj
                    let getter2: NumberGetter2 = obj
                    result = getter1.getNumber()
                    obj.x = 23
                    result = result + getter2.getNumber()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void throwsOnUnsatisfiedInterface() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn getNumber() -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter

                fn NumberImpl::getNumber() -> float { // wrong type
                    return (float) self.x
                }
                fn NumberImpl::getNumberFoo() -> int { // wrong name
                    return self.x
                }

                var result: int
                fn main() {
                    let obj: NumberGetter = new NumberImpl {
                        x = 123
                    }
                    result = obj.getNumber()
                }
                """);
        assertThatThrownBy(() -> run(modules))
                .hasCauseInstanceOf(CompilationErrorException.class)
                .hasMessageContaining("does not implement method");
    }

    @Test
    public void interfaceWithMultipleImplementations() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface IntOperator {
                    fn apply(a: int, b: int) -> int
                }

                struct Addition {} is IntOperator
                
                fn Addition::apply(a: int, b: int) -> int {
                    return a + b
                }

                struct Subtraction {} is IntOperator
                
                fn Subtraction::apply(a: int, b: int) -> int {
                    return a - b
                }

                fn op(operator: IntOperator, a: int, b: int) -> int {
                    return operator.apply(a, b)
                }

                var addResult: int
                var subResult: int

                fn main() {
                    addResult = op(new Addition {}, 100, 23)
                    subResult = op(new Subtraction {}, 123, 23)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("addResult")).isEqualTo(123);
        assertThat(globals.get("subResult")).isEqualTo(100);
    }

    @Test
    public void interfaceWithMultipleMethods() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface FloatOperations {
                    fn consume(a: float)
                    fn produce() -> float
                    fn unary(a: float) -> float
                    fn binary(a: float, b: float) -> float
                }
                
                struct NegativeOps {
                    f: float
                } is FloatOperations
                
                fn NegativeOps::consume(a: float) {
                    self.f = a
                }
                
                fn NegativeOps::produce() -> float {
                    return self.f
                }
                
                fn NegativeOps::unary(a: float) -> float {
                    return self.f - a
                }

                fn NegativeOps::binary(a: float, b: float) -> float {
                    return a - b
                }

                var result: float
                fn main() {
                    let foo: FloatOperations = new NegativeOps {}
                    foo.consume(100.0)
                    result = foo.binary(foo.produce(), foo.unary(60.0))
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(60.0);
    }
}
