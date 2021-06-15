package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.SourceFileLocation;
import net.smackem.zlang.modules.SourceFileLocations;
import org.junit.Test;

import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SemanticsTest {

    @Test
    public void hiddenForeignGlobalVariables() throws Exception {
        final String mainSource = """
                module main uses dep
                fn main() {
                    depY = 666
                }
                """;
        final String depSource = """
                var depY: int = 234
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep", depSource));
        final ParsedModule module = ParsedModule.parse("main", loc);
        assertThatThrownBy(() -> run(module.flatten())).hasMessageContaining("foreign variable");
    }

    @Test
    public void visibleForeignGlobalConstants() throws Exception {
        final String mainSource = """
                module main uses dep
                var result: int
                fn main() {
                    result = depY
                }
                """;
        final String depSource = """
                let depY: int = 123
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep", depSource));
        final ParsedModule module = ParsedModule.parse("main", loc);
        final Map<String, Object> globals = run(module.flatten());
        assertThat(globals.get("result")).isEqualTo(123);
    }

    @Test
    public void hiddenForeignStructCreation() throws Exception {
        final String mainSource = """
                module main uses dep
                fn main() {
                    let s: MyStruct = new MyStruct {}
                }
                """;
        final String depSource = """
                struct MyStruct {
                    f: int
                }
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep", depSource));
        final ParsedModule module = ParsedModule.parse("main", loc);
        assertThatThrownBy(() -> run(module.flatten())).hasMessageContaining("foreign type");
    }

    @Test
    public void hiddenForeignFields() throws Exception {
        final String mainSource = """
                module main uses dep
                fn main() {
                    let s: MyStruct = makeMyStruct(123)
                    s.f = 666
                }
                """;
        final String depSource = """
                struct MyStruct {
                    f: int
                }
                fn makeMyStruct(f: int) -> MyStruct {
                    return new MyStruct {
                        f: f
                    }
                }
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep", depSource));
        final ParsedModule module = ParsedModule.parse("main", loc);
        assertThatThrownBy(() -> run(module.flatten())).hasMessageContaining("field of foreign type");
    }
}
