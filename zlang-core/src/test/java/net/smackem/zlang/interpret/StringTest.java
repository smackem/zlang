package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;

public class StringTest {

    @Test
    public void stringLength() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var len0: int
                var size0: int
                var len3: int
                var size3: int
                fn main() {
                    let str0: string = ""
                    let str3: string = "abc"
                    size0 = str0.size()
                    len0 = str0.length()
                    size3 = str3.size()
                    len3 = str3.length()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("size0")).isEqualTo(1);
        assertThat(globals.get("len0")).isEqualTo(0);
        assertThat(globals.get("size3")).isEqualTo(4);
        assertThat(globals.get("len3")).isEqualTo(3);
    }

    @Test
    public void stringIndex() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var char0: byte
                var char1: byte
                var char2: byte
                var char3: byte
                var result: string
                fn main() {
                    let str: string = "abc"
                    char0 = str[0]
                    char1 = str[1]
                    char2 = str[2]
                    char3 = str[3]
                    str[0] = 'Z'
                    result = str
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("char0")).isEqualTo((byte) 'a');
        assertThat(globals.get("char1")).isEqualTo((byte) 'b');
        assertThat(globals.get("char2")).isEqualTo((byte) 'c');
        assertThat(globals.get("char3")).isEqualTo((byte) 0);
        assertThat(globals.get("result")).isEqualTo("Zbc");
    }

    @Test
    public void stringEquality() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let result: bool[] = new bool[5]
                fn main() {
                    let s1: string = "abc"
                    let s2: string = "def"
                    result[0] = s1 == "abc"
                    result[1] = s1 != s1
                    result[2] = s1 != nil
                    result[3] = s1 == s2
                    result[4] = s2 == "def"
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(new int[] { 1, 0, 1, 0, 1 });
    }

    @Test
    public void stringComparison() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var result: bool[]
                fn main() {
                    let s1: string = "abc"
                    let s2: string = "def"
                    result = new bool[] {
                        s1 < s2,
                        s1 >= s2,
                        s2 > s1,
                        s2 <= s1,
                        s1 > nil,
                        s1 <= nil
                    }
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(new int[] { 1, 0, 1, 0, 1, 0 });
    }

    @Test
    public void stringConcatenationBuffered() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var result: string
                fn main() {
                    let a: string = "abc"
                    let b: string = "."
                    let c: string = "def"
                    result = "" + a + b + c
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo("abc.def");
    }

    @Test
    public void stringConcatenation() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var result: string
                fn main() {
                    result = "" + "abc" + "." + "def"
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo("abc.def");
    }

    @Test
    public void stringConversion() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var s: string
                var a: byte[]
                fn main() {
                    s = (string) new byte[] { 'a', 'b', 'c', (byte) 0 }
                    a = (byte[]) s
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("s")).isEqualTo("abc");
        assertThat(globals.get("a")).isEqualTo(new byte[] { (byte) 'a', (byte) 'b', (byte) 'c', 0});
    }
}
