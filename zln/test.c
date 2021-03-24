//
// Created by smackem on 23.03.21.
//
#include <stdio.h>
#include <build_config.h>
#include <util.h>
#include <memory.h>
#include "emit.h"

static const RuntimeConfig config = { .register_count = 16, .max_stack_depth = 4};

// ---------------------------------------------------------------------
// TEST 1
// - load integer constants and store globals
// - add integers
// ---------------------------------------------------------------------

void test1(byte_t *code, MemoryLayout *heap) {
    // globals
    const addr_t glb_i0 = 0;
    const addr_t glb_i1 = 4;
    heap->global_segment_size = 8;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 100);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 200);
    code_ptr += emit_binary_op(code_ptr, OPC_Add_i32, 1, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i0);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 2, glb_i1);
    *code_ptr = OPC_Ret;

    // act
    execute(code, 0, 0, heap, &config);

    // assert
    const byte_t *global_segment = heap->base + heap->const_segment_size;
    assert_equal(get_int(global_segment, glb_i0), 300, "i0");
    assert_equal(get_int(global_segment, glb_i1), 200, "i1");
}

// ---------------------------------------------------------------------
// TEST 2
// - load constants from const segment
// ---------------------------------------------------------------------

void test2(byte_t *code, MemoryLayout *heap) {
    // constants
    const addr_t const_f1 = 0;
    const addr_t const_f2 = 8;
    set_float(heap->base, const_f1, 1000.125);
    set_float(heap->base, const_f2, 123.5);
    heap->const_segment_size = 16;

    // globals
    const addr_t glb_f1 = 0;
    const addr_t glb_f2 = 8;
    heap->global_segment_size = 16;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 1, const_f1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f1);
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 1, const_f2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f2);
    *code_ptr = OPC_Ret;

    // act
    execute(code, 0, 0, heap, &config);

    // assert
    const byte_t *global_segment = heap->base + heap->const_segment_size;
    assert_equal(get_float(global_segment, glb_f1), 1000.125, "f1");
    assert_equal(get_float(global_segment, glb_f2), 123.5, "f2");
}

// ---------------------------------------------------------------------
// TEST 3
// - load and store globals
// - more integer arithmetics
// ---------------------------------------------------------------------

void test3(byte_t *code, MemoryLayout *heap) {
    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;
    heap->global_segment_size = 12;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 100);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 30);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i2);
    code_ptr += emit_reg_addr(code_ptr, OPC_LdGlb_i32, 1, glb_i1);
    code_ptr += emit_reg_addr(code_ptr, OPC_LdGlb_i32, 2, glb_i2);
    code_ptr += emit_binary_op(code_ptr, OPC_Sub_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i1);
    code_ptr += emit_binary_op(code_ptr, OPC_Mul_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i2);
    code_ptr += emit_binary_op(code_ptr, OPC_Div_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i3);
    *code_ptr = OPC_Ret;

    // act
    execute(code, 0, 0, heap, &config);

    // assert
    const byte_t *global_segment = heap->base + heap->const_segment_size;
    assert_equal(get_int(global_segment, glb_i1), 70, "i1");
    assert_equal(get_int(global_segment, glb_i2), 3000, "i2");
    assert_equal(get_int(global_segment, glb_i3), 3, "i3");
}

// ---------------------------------------------------------------------
// TEST entry point
// ---------------------------------------------------------------------

static const struct test {
    void (*proc)(byte_t *, MemoryLayout *);
    const char *name;
} tests[] = {
        { .proc = test1, .name = "load integer constants, add integers, store globals" },
        { .proc = test2, .name = "load f64 constant" },
        { .proc = test3, .name = "load and store globals, more integer arithmetic" },
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
    }

    fprintf(stdout, "<<< tests completed successfully\n");
    return 0;
}
