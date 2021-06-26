package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;

public class DebugTest {

    @Test
    public void assertFailedStmt() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                var numberB: int
                fn main() {
                    numberA = 100
                    assert numberA != 100
                    numberB = 666
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(100);
        assertThat(globals.get("numberB")).isEqualTo(0);
    }

    @Test
    public void assertStmt() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                var numberB: int
                fn main() {
                    numberA = 100
                    assert numberA == 100
                    numberB = 666
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(100);
        assertThat(globals.get("numberB")).isEqualTo(666);
    }
}
