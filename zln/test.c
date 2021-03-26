//
// Created by smackem on 23.03.21.
//
#include <stdio.h>
#include <build_config.h>
#include <util.h>
#include <memory.h>
#include "emit.h"

static RuntimeConfig config = { .register_count = 8, .max_stack_depth = 4 };

static void dump_cpu(addr_t pc,
                     addr_t base_pc,
                     const Instruction *instr,
                     size_t stack_depth,
                     const StackFrame *stack_frame,
                     const Register *registers,
                     size_t register_count) {
    fprintf(stdout, "-------- [%lu]\n", stack_depth);
    fprintf(stdout, "%08x ", base_pc + pc);
    print_instruction(stdout, instr);
    fputc('\n', stdout);
    print_registers(stdout, registers, register_count);
}

// ---------------------------------------------------------------------
// TEST 1
// - load integer constants and store globals
// - add integers
// ---------------------------------------------------------------------

void test01(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i0 = 0;
    const addr_t glb_i1 = 4;
    memory->global_segment_size = 8;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 100);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 200);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_Add_i32, 1, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i0);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 2, glb_i1);
    *code_ptr = OPC_Ret;

    // act
    execute(code, 0, 0, memory, &config);

    // assert_that
    const byte_t *global_segment = memory->base + memory->const_segment_size;
    assert_equal(get_int(global_segment, glb_i0), 300, "i0");
    assert_equal(get_int(global_segment, glb_i1), 200, "i1");
}

// ---------------------------------------------------------------------
// TEST 2
// - load constants from const segment
// ---------------------------------------------------------------------

void test02(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_f1 = 0;
    const addr_t const_f2 = 8;
    set_float(memory->base, const_f1, 1000.125);
    set_float(memory->base, const_f2, 123.5);
    memory->const_segment_size = 16;

    // globals
    const addr_t glb_f1 = 0;
    const addr_t glb_f2 = 8;
    memory->global_segment_size = 16;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 1, const_f1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f1);
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 1, const_f2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f2);
    *code_ptr = OPC_Ret;

    // act
    execute(code, 0, 0, memory, &config);

    // assert_that
    const byte_t *global_segment = memory->base + memory->const_segment_size;
    assert_equal(get_float(global_segment, glb_f1), 1000.125, "f1");
    assert_equal(get_float(global_segment, glb_f2), 123.5, "f2");
}

// ---------------------------------------------------------------------
// TEST 3
// - load and store globals
// - more integer arithmetics
// ---------------------------------------------------------------------

void test03(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;
    memory->global_segment_size = 12;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 100);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 30);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i2);
    code_ptr += emit_reg_addr(code_ptr, OPC_LdGlb_i32, 1, glb_i1);
    code_ptr += emit_reg_addr(code_ptr, OPC_LdGlb_i32, 2, glb_i2);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_Sub_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i1);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_Mul_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i2);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_Div_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i3);
    *code_ptr = OPC_Ret;

    // act
    execute(code, 0, 0, memory, &config);

    // assert_that
    const byte_t *global_segment = memory->base + memory->const_segment_size;
    assert_equal(get_int(global_segment, glb_i1), 70, "i1");
    assert_equal(get_int(global_segment, glb_i2), 3000, "i2");
    assert_equal(get_int(global_segment, glb_i3), 3, "i3");
}

// ---------------------------------------------------------------------
// TEST 4
// - branching, loops
// ---------------------------------------------------------------------

void test04(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;
    memory->global_segment_size = 4;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 100);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 3, 1);
    addr_t label_loop = code_ptr - code;
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_Add_i32, 1, 1, 3);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_Eq_i32, 4, 1, 2);
    code_ptr += emit_reg_int(code_ptr, OPC_Br_False, 4, label_loop);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    *code_ptr = OPC_Ret;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    execute(code, 0, 0, memory, &config);

    // assert_that
    const byte_t *global_segment = memory->base + memory->const_segment_size;
    assert_equal(get_int(global_segment, glb_i1), 100, "i1");
}

// ---------------------------------------------------------------------
// TEST 5
// - boolean operators, mov
// ---------------------------------------------------------------------

void test05(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;
    memory->global_segment_size = 12;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, true);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, false);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_And, 3, 1, 2);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_Or, 4, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 4, glb_i2);
    code_ptr += emit_reg_reg(code_ptr, OPC_Mov, 3, 4);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i3);
    *code_ptr = OPC_Ret;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    config.debug_callback = dump_cpu;
    execute(code, 0, 0, memory, &config);

    // assert_that
    const byte_t *global_segment = memory->base + memory->const_segment_size;
    assert_equal(get_int(global_segment, glb_i1), false, "i1");
    assert_equal(get_int(global_segment, glb_i2), true, "i2");
    assert_equal(get_int(global_segment, glb_i3), true, "i3");
}

// ---------------------------------------------------------------------
// TEST 6
// - type conversion
// ---------------------------------------------------------------------

void test06(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_f1 = 0;
    set_float(memory->base, const_f1, 1000.125);
    memory->const_segment_size = 8;

    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;
    const addr_t glb_f1 = 12;
    memory->global_segment_size = 20;

    // code
    byte_t *code_ptr = code;
    // i1 <- (u8)0x123ab
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x123ab);
    code_ptr += emit_reg_reg_addr(code_ptr, OPC_Conv_i32, 1, 1, TYPE_Unsigned8);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    // f1 <- (f64)0x123ab
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 123);
    code_ptr += emit_reg_reg_addr(code_ptr, OPC_Conv_i32, 1, 1, TYPE_Float64);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f1);
    // i2 <- (i32)1000.125
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_f64, 1, const_f1);
    code_ptr += emit_reg_reg_addr(code_ptr, OPC_Conv_f64, 1, 1, TYPE_Int32);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i2);
    // i3 <- (ref)0x123ab
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x123ab);
    code_ptr += emit_reg_reg_addr(code_ptr, OPC_Conv_i32, 1, 1, TYPE_Ref);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_ref, 1, glb_i3);
    *code_ptr = OPC_Ret;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    execute(code, 0, 0, memory, &config);

    // assert_that
    const byte_t *global_segment = memory->base + memory->const_segment_size;
    assert_equal(get_int(global_segment, glb_i1), 0xab, "i1");
    assert_equal(get_float(global_segment, glb_f1), 123.0, "f1");
    assert_equal(get_int(global_segment, glb_i2), 1000, "i2");
    assert_equal(get_addr(global_segment, glb_i3), 0x123ab, "i3");
}

// ---------------------------------------------------------------------
// TEST 7
// - array allocation and element access
// ---------------------------------------------------------------------

void test07(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;
    memory->global_segment_size = 12;

    // code
    byte_t *code_ptr = code;
    // i1 <- new int[10]
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 10);
    code_ptr += emit_reg_reg(code_ptr, OPC_NewArr_i32, 2, 1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 2, glb_i1);
    // i2 <- new byte[20]
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 20);
    code_ptr += emit_reg_reg(code_ptr, OPC_NewArr_u8, 3, 1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i2);
    // i1[5] <- 123
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_ref, 1, 5);
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_i32, 4, 123);
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_StElem_i32, 4, 2, 1);
    // i3 <- i1[5]
    code_ptr += emit_reg_reg_reg(code_ptr, OPC_LdElem_i32, 4, 2, 1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 4, glb_i3);
    *code_ptr = OPC_Ret;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    execute(code, 0, 0, memory, &config);

    // assert_that
    const byte_t *global_segment = memory->base + memory->const_segment_size;
    const byte_t *heap_segment = global_segment + memory->global_segment_size;
    assert_equal(get_int(global_segment, glb_i1), 0, "i1 (addr of first array)");
    assert_equal(((HeapEntry *)heap_segment)->header, 40, "heap_entry_1.size");
    assert_equal(((HeapEntry *)heap_segment)->ref_count, 1, "heap_entry_1.refcount");
    assert_equal(get_int(global_segment, glb_i2), 40 + HEAP_ENTRY_HEADER_SIZE, "i2 (addr of second array)");
    assert_equal(((HeapEntry *)&heap_segment[40 + HEAP_ENTRY_HEADER_SIZE])->header, 20, "heap_entry_2.size");
    assert_equal(((HeapEntry *)&heap_segment[40 + HEAP_ENTRY_HEADER_SIZE])->ref_count, 1, "heap_entry_2.refcount");
    assert_equal(get_int(global_segment, glb_i3), 123, "i3");
}

// ---------------------------------------------------------------------
// TEST 8
// - struct allocation and field access
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 9
// - branching in multi-module program (within single module)
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 10
// - function call `function f()`
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 11
// - function call `function f() -> int`
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 12
// - function call `function f(int, double) -> ref`
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 13
// - function call `function f(int, double) -> ref` across modules
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 14
// - object allocation
// - reference counting within single stack frame
// - object de-allocation
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 15
// - reference counting across multiple stack frames
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 16
// - heap compaction
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 17
// - recursive function call `function f(int) -> int`
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 18
// - strings
// ---------------------------------------------------------------------


// ---------------------------------------------------------------------
// TEST entry point
// ---------------------------------------------------------------------

static const struct test {
    void (*proc)(byte_t *, MemoryLayout *);
    const char *name;
} tests[] = {
        { .proc = test01, .name = "load integer constants, add integers, store globals" },
        { .proc = test02, .name = "load f64 constant" },
        { .proc = test03, .name = "load and store globals, more integer arithmetic" },
        { .proc = test04, .name = "branching and loops" },
        { .proc = test05, .name = "boolean operators, mov" },
        { .proc = test06, .name = "type conversion" },
        { .proc = test07, .name = "array allocation and element access" },
        { .proc = NULL },
};

static byte_t heap_memory[1024];
static byte_t code_memory[1024];

int main() {
    MemoryLayout heap;
    fprintf(stdout, "zln_test v%d.%d\n", zln_VERSION_MAJOR, zln_VERSION_MINOR);
    fprintf(stdout, ">>> running tests...\n");
    int n = 1;

    for (const struct test *test_ptr = tests; test_ptr->proc != NULL; test_ptr++, n++) {
        fprintf(stdout, "%d) %s\n", n, test_ptr->name);
        bzero(heap_memory, sizeof(heap_memory));
        bzero(code_memory, sizeof(code_memory));
        bzero(&heap, sizeof(heap));
        heap.base = heap_memory;
        heap.total_size = sizeof(heap_memory);
        test_ptr->proc(code_memory, &heap);
        config.debug_callback = NULL;
    }

    fprintf(stdout, "<<< tests completed successfully\n");
    return 0;
}
