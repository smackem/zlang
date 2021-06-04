package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
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
                    let obj: NumberGetterImpl = new NumberGetterImpl {
                        x: 123
                    }
                    result = obj.get_number()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(123);
    }
}
