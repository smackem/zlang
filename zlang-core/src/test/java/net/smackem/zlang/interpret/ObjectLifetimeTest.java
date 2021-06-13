package net.smackem.zlang.interpret;

import com.google.common.io.Files;
import net.smackem.zlang.emit.bytecode.ByteCode;
import net.smackem.zlang.emit.bytecode.HeapEntry;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.assertj.core.groups.Tuple;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

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
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(40, 0, "Int32[]"));
    }

    @Test
    public void singleArrayGlobal() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                let x: int[] = new int[10]
                fn main() {
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(40, 1, "Int32[]"));
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
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(4, 1, "SomeType"));
    }

    @Test
    public void graphWithSingleGlobalRoot() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct SomeType {
                    others: OtherType[]
                }
                struct OtherType {
                    array: byte[]
                }
                let x: SomeType = new SomeType {
                    others: new OtherType[] {
                        new OtherType {
                            array: new byte[10]
                        }
                    }
                }
                fn main() {
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(
                        Tuple.tuple(4, 1, "SomeType"),
                        Tuple.tuple(4, 1, "Ref[]"),
                        Tuple.tuple(4, 1, "OtherType"),
                        Tuple.tuple(10, 1, "Unsigned8[]"));
    }

    @Test
    public void graphOnStack() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct SomeType {
                    others: OtherType[]
                }
                struct OtherType {
                    array: byte[]
                }
                fn main() {
                    let x: SomeType = new SomeType {
                        others: new OtherType[] {
                            new OtherType {
                                array: new byte[10]
                            }
                        }
                    }
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(
                        Tuple.tuple(4, 0, "SomeType"),
                        Tuple.tuple(4, 0, "Ref[]"),
                        Tuple.tuple(4, 0, "OtherType"),
                        Tuple.tuple(10, 0, "Unsigned8[]"));
    }

    @Test
    public void refCountOnStack() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                fn main() {
                    let a: byte[] = new byte[0]
                    let b: byte[] = a
                    let c: byte[] = b
                    panic 0 // break execution without cleaning up
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(0, 3, "Unsigned8[]"));
    }

    @Test
    public void refCountOnStackWithCleanup() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                fn main() {
                    let a: byte[] = new byte[0]
                    let b: byte[] = a
                    let c: byte[] = b
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(0, 0, "Unsigned8[]"));
    }

    @Test
    public void refCountPerStackFrame() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                fn pass1(a: byte[]) {
                    pass2(a)
                }
                fn pass2(a: byte[]) {
                    let b: byte[] = a
                    panic 0 // break execution without cleaning up
                }
                fn main() {
                    let a: byte[] = new byte[0]
                    pass1(a)
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(0, 4, "Unsigned8[]"));
    }

    @Test
    public void refCountPerStackFrameWithCleanup() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                fn pass1(a: byte[]) {
                    pass2(a)
                }
                fn pass2(a: byte[]) {
                    let b: byte[] = a
                }
                fn main() {
                    let a: byte[] = new byte[0]
                    pass1(a)
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(0, 0, "Unsigned8[]"));
    }

    @Test
    public void reuseHeapSlot() throws Exception {
        // allocate complete heap for each object, minus header
        final int blobSize = InterpreterTests.HEAP_SIZE - (ByteCode.HEAP_ENTRY_HEADER_SIZE + ByteCode.HEAP_RESERVED_BYTES);
        final List<ParsedModule> modules = ParsedModules.single("""
                fn alloc() {
                    let a: byte[] = new byte[%d]
                }
                fn main() {
                    for i: int in 0 .. 10 {
                        alloc()
                    }
                }
                """.formatted(blobSize));
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(Tuple.tuple(blobSize, 0, "Unsigned8[]"));
    }

    @Test
    public void splitHeapSlot() throws Exception {
        // allocate complete heap for each object, minus header
        final int blobSize = InterpreterTests.HEAP_SIZE - (ByteCode.HEAP_ENTRY_HEADER_SIZE + ByteCode.HEAP_RESERVED_BYTES);
        final List<ParsedModule> modules = ParsedModules.single("""
                fn consume(a: byte[]) -> byte[] {
                    return a
                }
                fn main() {
                    consume(new byte[%d])
                    let a: byte[] = consume(new byte[10])
                    let b: byte[] = consume(new byte[100])
                }
                """.formatted(blobSize));
        InterpreterTests.writeZap(modules, Paths.get(System.getProperty("user.home"), "splitHeapSlot.zap"));
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(
                        Tuple.tuple(10, 0, "Unsigned8[]"),
                        Tuple.tuple(100, 0, "Unsigned8[]"));
    }

    @Test
    public void compactHeap() throws Exception {
        final int blobSize = InterpreterTests.HEAP_SIZE - (ByteCode.HEAP_ENTRY_HEADER_SIZE + ByteCode.HEAP_RESERVED_BYTES);
        final int smallObjSize = blobSize / 10;
        final int largeObjSize = smallObjSize * 5;
        final List<ParsedModule> modules = ParsedModules.single("""
                fn consume(a: byte[]) -> byte[] {
                    return a
                }
                fn main() {
                    for i: int in 0 .. 10 {
                        consume(new byte[%d])
                    }
                    let a: byte[] = new byte[%d]
                }
                """.formatted(smallObjSize, largeObjSize));
        InterpreterTests.writeZap(modules, Paths.get(System.getProperty("user.home"), "compactHeap.zap"));
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(
                        Tuple.tuple(largeObjSize, 0, "Unsigned8[]"));
    }
}
