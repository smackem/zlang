package net.smackem.zlang.interpret;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static org.assertj.core.api.Assertions.assertThat;

public class BuiltInCollectionsTest {

    @Test
    public void simpleArray() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let a: int[] = new int[3]
                var a1: int
                var a2: int
                var a3: int
                fn main() {
                    a[0] = 1
                    a[1] = 2
                    a[2] = 3
                    a1 = a[0]
                    a2 = a[1]
                    a3 = a[2]
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("a1")).isEqualTo(1);
        assertThat(globals.get("a2")).isEqualTo(2);
        assertThat(globals.get("a3")).isEqualTo(3);
    }

    @Test
    public void moreArrays() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let ints: int[] = new int[2]
                let floats: float[] = new float[2]
                let bytes: byte[] = new byte[2]
                let arrays: bool[][] = new bool[][2]
                var f0: float
                var i0: int
                var a0: bool[]
                var a1: bool[]

                fn main() {
                    ints[0] = 1
                    ints[1] = 2
                    i0 = ints[0]
                    floats[0] = 12.5
                    floats[1] = 13.5
                    f0 = floats[0]
                    bytes[0] = (byte) 10
                    bytes[1] = (byte) 11
                    arrays[0] = new bool[2]
                    arrays[0][0] = true
                    arrays[0][1] = false
                    arrays[1] = new bool[2]
                    arrays[1][0] = false
                    arrays[1][1] = true
                    a0 = arrays[0]
                    a1 = arrays[1]
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("i0")).isEqualTo(1);
        assertThat(globals.get("ints")).isEqualTo(new int[] { 1, 2 });
        assertThat(globals.get("f0")).isEqualTo(12.5);
        assertThat(globals.get("floats")).isEqualTo(new double[] { 12.5, 13.5 });
        assertThat(globals.get("bytes")).isEqualTo(new byte[] { 10, 11 });
        assertThat(globals.get("a0")).isEqualTo(new int[] { 1, 0 });
        assertThat(globals.get("a1")).isEqualTo(new int[] { 0, 1 });
    }

    @Test
    public void builtInArrayMethods() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var a: int
                var b: int
                var t1: int[]
                var t2: int[]
                var c: int
                fn main() {
                    let source: int[] = new int[3]
                    source[0] = 1
                    source[1] = 2
                    source[2] = 3
                    a = source.size()
                    b = new float[0].size()
                    t1 = source.copy(1, 2)
                    t2 = source.copy(2, 10)
                    c = t2.size()
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("a")).isEqualTo(3);
        assertThat(globals.get("b")).isEqualTo(0);
        assertThat(globals.get("c")).isEqualTo(10);
        assertThat(globals.get("t1")).isEqualTo(new int[] { 2, 3 });
        assertThat(globals.get("t2")).isEqualTo(new int[] { 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
    }

    @Test
    public void listCreateAndGet() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var a: int
                var b: int
                var c: int
                var size: int
                var capacity: int
                fn main() {
                    let l: int list = new int list { 1, 2, 3 }
                    size = l.size()
                    capacity = l.capacity()
                    a = l[0]
                    b = l[1]
                    c = l[2]
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("a")).isEqualTo(1);
        assertThat(globals.get("b")).isEqualTo(2);
        assertThat(globals.get("c")).isEqualTo(3);
        assertThat(globals.get("capacity")).isEqualTo(16);
        assertThat(globals.get("size")).isEqualTo(3);
    }

    @Test
    public void listAddAndSet() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let l: int list = new int list {}
                var capacity: int
                var size: int
                fn main() {
                    for i: int in 0 .. 100 {
                        l.add(i)
                    }
                    l[4] = 666
                    l[5] = 777
                    capacity = l.capacity()
                    size = l.size()
                }
                """);
        final Map<String, Object> globals = run(modules);
        //noinspection unchecked
        final Map<String, Object> list = (Map<String, Object>) globals.get("l");
        assertThat((int[]) list.get("@array")).startsWith(0, 1, 2, 3, 666, 777);
        assertThat(globals.get("capacity")).isEqualTo(112);
        assertThat(globals.get("size")).isEqualTo(100);
    }

    @Test
    public void listCreateFromArray() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var l: int list
                var size: int
                fn main() {
                    let a: int[] = new int[] { 1, 2, 3 }
                    l = new int list(a)
                    a[2] = 666
                    size = l.size()
                }
                """);
        final Map<String, Object> globals = run(modules);
        //noinspection unchecked
        final Map<String, Object> list = (Map<String, Object>) globals.get("l");
        assertThat(list.get("@array")).isEqualTo(new int[] { 1, 2, 666 });
        assertThat(globals.get("size")).isEqualTo(3);
    }

    @Test
    public void forIteratorList() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                var result: int
                var count: int
                fn main() {
                    let numbers: int list = new int list {
                        1, 2, 3
                    }
                    var n: int
                    var x: int
                    for i: int in numbers {
                        n = n + i
                        x = x + 1
                    }
                    result = n
                    count = x
                }
                """);
        final Map<String, Object> globals = run(modules);
        assertThat(globals.get("result")).isEqualTo(6);
        assertThat(globals.get("count")).isEqualTo(3);
    }
}
