package net.smackem.zlang.interpret;

import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Ignore;
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
                    fn get_number() -> int
                }

                struct NumberGetterImpl {
                    x: int
                } is NumberGetter

                fn NumberGetterImpl::get_number() -> int {
                    return self.x
                }

                var result: int
                fn main() {
                    let obj: NumberGetter = new NumberGetterImpl {
                        x: 123
                    }
                    result = obj.get_number()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void multipleInterfaceStaticDispatch() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn get_number() -> int
                }
                interface NumberSetter {
                    fn set_number(n: int) -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter, NumberSetter

                fn NumberImpl::get_number() -> int {
                    return self.x
                }

                fn NumberImpl::set_number(n: int) -> int {
                    self.x = n
                    return n
                }

                var result: int
                fn main() {
                    let obj: NumberImpl = new NumberImpl {
                        x: 0
                    }
                    obj.set_number(123)
                    result = obj.get_number()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void dynamicDispatchParameter() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn get_number() -> int
                }

                struct NumberGetterImpl {
                    x: int
                } is NumberGetter

                fn NumberGetterImpl::get_number() -> int {
                    return self.x
                }
                
                fn get_number_from(getter: NumberGetter) -> int {
                    return getter.get_number()
                }

                var result: int
                fn main() {
                    let obj: NumberGetterImpl = new NumberGetterImpl {
                        x: 123
                    }
                    result = get_number_from(obj)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void multipleInterfaceDynamicDispatch() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn get_number() -> int
                }
                interface NumberSetter {
                    fn set_number(n: int) -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter, NumberSetter

                fn NumberImpl::get_number() -> int {
                    return self.x
                }

                fn NumberImpl::set_number(n: int) -> int {
                    self.x = n
                    return n
                }

                var result: int
                fn main() {
                    let obj: NumberImpl = new NumberImpl {
                        x: 0
                    }
                    let getter: NumberGetter = obj
                    let setter: NumberSetter = obj
                    setter.set_number(123)
                    result = getter.get_number()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void multipleInterfaceDynamicDispatchDoubleImpl() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter1 {
                    fn get_number() -> int
                }
                interface NumberGetter2 {
                    fn get_number() -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter1, NumberGetter2

                fn NumberImpl::get_number() -> int {
                    return self.x
                }

                var result: int
                fn main() {
                    let obj: NumberImpl = new NumberImpl {
                        x: 100
                    }
                    let getter1: NumberGetter1 = obj
                    let getter2: NumberGetter2 = obj
                    result = getter1.get_number()
                    obj.x = 23
                    result = result + getter2.get_number()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void throwsOnUnsatisfiedInterface() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface NumberGetter {
                    fn get_number() -> int
                }

                struct NumberImpl {
                    x: int
                } is NumberGetter

                fn NumberImpl::get_number() -> float { // wrong type
                    return (float) self.x
                }
                fn NumberImpl::get_number_foo() -> int { // wrong name
                    return self.x
                }

                var result: int
                fn main() {
                    let obj: NumberGetter = new NumberImpl {
                        x: 123
                    }
                    result = obj.get_number()
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

                var add_result: int
                var sub_result: int

                fn main() {
                    add_result = op(new Addition {}, 100, 23)
                    sub_result = op(new Subtraction {}, 123, 23)
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("add_result")).isEqualTo(123);
        assertThat(globals.get("sub_result")).isEqualTo(100);
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
