package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;

public class ObjectLifetimeTest {

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
}
