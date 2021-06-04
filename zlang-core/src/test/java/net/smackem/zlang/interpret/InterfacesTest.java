package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;

public class InterfacesTest {
    @Test
    public void simpleInterfaceImpl() throws Exception {
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
    public void multipleInterfaceImpl() throws Exception {
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
    @Ignore
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
}
