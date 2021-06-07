package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.HeapEntry;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.assertj.core.groups.Tuple;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static net.smackem.zlang.interpret.InterpreterTests.run;
import static net.smackem.zlang.interpret.InterpreterTests.runExtractingHeap;
import static org.assertj.core.api.Assertions.assertThat;

public class ObjectLifetimeTest {

    @Test
    public void singleArrayOnStack() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                fn main() {
                    let x: int[] = new int[10]
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(40, 0, "Int32[]"));
        System.out.println(heap);
    }

    @Test
    public void singleArrayGlobal() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let x: int[] = new int[10]
                fn main() {
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(40, 1, "Int32[]"));
        System.out.println(heap);
    }

    @Test
    public void singleListGlobal() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct SomeType {
                    n: int
                }
                let x: SomeType = new SomeType {}
                fn main() {
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(4, 1, "SomeType"));
        System.out.println(heap);
    }
}
