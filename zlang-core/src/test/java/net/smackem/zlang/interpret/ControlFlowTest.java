package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;

public class ControlFlowTest {

    @Test
    public void simpleWhileLoop() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var number: int
                fn main() {
                    var x: int = 0
                    let delta: int = 1
                    while x <= 10 {
                        number = number + x
                        x = x + delta
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("number")).isEqualTo(1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10);
    }

    @Test
    public void simpleIf() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var number: int
                fn main() {
                    var x: int = 0
                    let delta: int = 1
                    if delta > 0 {
                        number = 123
                    }
                    if delta < 0 {
                        number = 666
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("number")).isEqualTo(123);
    }

    @Test
    public void ifElse() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                var numberB: int
                fn main() {
                    let delta: int = 1
                    if delta > 0 {
                        numberA = 123
                    } else {
                        numberB = 666
                    }
                    if delta < 0 {
                        numberB = 666
                    } else {
                        numberB = 234
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(123);
        assertThat(globals.get("numberB")).isEqualTo(234);
    }

    @Test
    public void ifElseIf() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var numberA: int
                var numberB: int
                var numberC: int
                fn main() {
                    var delta: int
                    if delta == 0 {
                        numberA = 123
                    } else if delta == 1 {
                        numberA = 0
                    } else {
                        numberA = -1
                    }
                    delta = 1
                    if delta == 0 {
                        numberB = 0
                    } else if delta == 1 {
                        numberB = 234
                    } else {
                        numberB = -1
                    }
                    delta = 2
                    if delta == 0 {
                        numberC = 0
                    } else if delta == 1 {
                        numberC = 0
                    } else {
                        numberC = 345
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("numberA")).isEqualTo(123);
        assertThat(globals.get("numberB")).isEqualTo(234);
        assertThat(globals.get("numberC")).isEqualTo(345);
    }

    @Test
    public void forRangeStmt() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var a: int
                fn main() {
                    var n: int
                    for i: int in 0 .. 10 {
                        n = n + i
                    }
                    a = n
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("a")).isEqualTo(45);
    }

    @Test
    public void forIterator() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var result: int
                fn main() {
                    let array: int[] = new int[] {
                        1, 2, 3
                    }
                    var n: int
                    var x: int
                    for i: int in array {
                        n = n + i
                    }
                    result = n
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(6);
    }
}
