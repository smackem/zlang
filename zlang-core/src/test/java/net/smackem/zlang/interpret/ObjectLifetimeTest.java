package net.smackem.zlang.interpret;

import net.smackem.zlang.emit.bytecode.ByteCode;
import net.smackem.zlang.emit.bytecode.HeapEntry;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import org.assertj.core.groups.Tuple;
import org.junit.Ignore;
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
                .containsExactly(Tuple.tuple(40, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Int32[]"));
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
                    others = new OtherType[] {
                        new OtherType {
                            array = new byte[10]
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
                        others = new OtherType[] {
                            new OtherType {
                                array = new byte[10]
                            }
                        }
                    }
                }
                """);
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(
                        Tuple.tuple(4, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "SomeType"),
                        Tuple.tuple(4, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Ref[]"),
                        Tuple.tuple(4, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "OtherType"),
                        Tuple.tuple(10, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Unsigned8[]"));
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
                .containsExactly(Tuple.tuple(0, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Unsigned8[]"));
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
                .containsExactly(Tuple.tuple(0, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Unsigned8[]"));
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
                .containsExactly(Tuple.tuple(blobSize, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Unsigned8[]"));
    }

    @Test
    public void splitHeapSlot() throws Exception {
        // allocate complete heap for each object, minus header
        final int blobSize = InterpreterTests.HEAP_SIZE - (ByteCode.HEAP_ENTRY_HEADER_SIZE + ByteCode.HEAP_RESERVED_BYTES);
        final List<ParsedModule> modules = ParsedModules.single("""
                fn consume(a: byte[]) -> byte[] {
                    return a
                }
                fn nop() {
                    // call this just to emit OpCode.Collect
                }
                fn main() {
                    consume(new byte[%d])
                    nop()
                    let a: byte[] = consume(new byte[10])
                    nop()
                    let b: byte[] = consume(new byte[100])
                }
                """.formatted(blobSize));
        InterpreterTests.writeZap(modules, Paths.get(System.getProperty("user.home"), "splitHeapSlot.zap"));
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(
                        Tuple.tuple(10, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Unsigned8[]"),
                        Tuple.tuple(100, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Unsigned8[]"));
    }

    @Test
    public void compactHeap() throws Exception {
        final int blobSize = InterpreterTests.HEAP_SIZE - ByteCode.HEAP_RESERVED_BYTES;
        final int smallObjSize = blobSize / 10 - ByteCode.HEAP_ENTRY_HEADER_SIZE;
        final int largeObjSize = smallObjSize * 5;
        final List<ParsedModule> modules = ParsedModules.single("""
                fn consumeMemory() {
                    let size: int = %d
                    let a0: byte[] = new byte[size]
                    let a1: byte[] = new byte[size]
                    let a2: byte[] = new byte[size]
                    let a3: byte[] = new byte[size]
                    let a4: byte[] = new byte[size]
                    let a5: byte[] = new byte[size]
                    let a6: byte[] = new byte[size]
                    let a7: byte[] = new byte[size]
                    let a8: byte[] = new byte[size]
                    let a9: byte[] = new byte[size]
                }
                fn main() {
                    consumeMemory()
                    let a: byte[] = new byte[%d]
                }
                """.formatted(smallObjSize, largeObjSize));
        InterpreterTests.writeZap(modules, Paths.get(System.getProperty("user.home"), "compactHeap.zap"));
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        assertThat(heap).extracting(HeapEntry::dataSize, HeapEntry::refCount, HeapEntry::typeName)
                .containsExactly(
                        Tuple.tuple(largeObjSize, ByteCode.FREE_HEAP_ENTRY_REF_COUNT, "Unsigned8[]"));
    }

    @Test
    public void stringAllocationsMustNotCrashVM() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                fn main() {
                    for i: int in 0 .. 500 {
                        let a: string = "0abcdefgh0"
                        let b: string = "1abcdefgh1"
                        let c: string = "2abcdefgh2"
                        let d: string = "3abcdefgh3"
                        let ab: string = a + b
                        let bc: string = b + c
                        let cd: string = c + d
                        let x: string = ab + bc + cd
                        log i
                    }
                }
                """);
        InterpreterTests.writeZap(modules, Paths.get(System.getProperty("user.home"), "stringAllocationsMustNotCrashVM.zap"));
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        // no VM crash (out of memory) expected
    }

    @Test
    public void arrayAllocationsMustNotCrashVM() throws Exception {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct Container {
                    c: byte[]
                    d: byte[]
                }
                fn main() {
                    let arr: byte[][] = new byte[][2]
                    let container: Container = new Container {}
                    for i: int in 0 .. 500 {
                        let a: byte[] = new byte[100]
                        let b: byte[] = new byte[10]
                        container.c = new byte[75]
                        container.d = new byte[12]
                        arr[0] = new byte[45]
                        arr[1] = new byte[7]
                        log i
                    }
                }
                """);
        InterpreterTests.writeZap(modules, Paths.get(System.getProperty("user.home"), "arrayAllocationsMustNotCrashVM.zap"));
        final Collection<HeapEntry> heap = runExtractingHeap(modules);
        System.out.println(heap);
        // no VM crash (out of memory) expected
    }
}
