//
// Created by smackem on 22.03.21.
//

#include <runtime.h>
#include <printf.h>

size_t emit_reg_int(byte_t *code, OpCode opc, byte_t r_target, int32_t i) {
    code[0] = opc;
    code[1] = r_target;
    set_int(code, 2, i);
    return 6;
}

size_t emit_3reg(byte_t *code, OpCode opc, byte_t r_target, byte_t r_left, byte_t r_right) {
    code[0] = opc;
    code[1] = r_target;
    code[2] = r_left;
    code[3] = r_right;
    return 4;
}

size_t emit_reg_addr(byte_t *code, OpCode opc, byte_t r_source, addr_t addr) {
    code[0] = opc;
    code[1] = r_source;
    set_addr(code, 2, addr);
    return 6;
}

static byte_t heap_memory[1024];
#define CONST_F0 0
#define GLB_I0 0
#define GLB_I1 4
#define GLB_F0 8

size_t emit_test_program(byte_t *code, size_t code_size, HeapLayout *heap) {
    // heap
    heap->memory = heap_memory;
    heap->total_size = sizeof(heap_memory);

    // constants
    set_float(heap->memory, CONST_F0, 1000.125);
    heap->const_segment_size = 8;

    // globals
    heap->global_segment_size = 8;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_LD_C_I32, 1, 100);
    code_ptr += emit_reg_int(code_ptr, OPC_LD_C_I32, 2, 200);
    code_ptr += emit_3reg(code_ptr, OPC_ADD_I32, 1, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_ST_GLB_I32, 1, GLB_I0);
    code_ptr += emit_reg_addr(code_ptr, OPC_ST_GLB_I32, 2, GLB_I1);
    code_ptr += emit_reg_addr(code_ptr, OPC_LD_C_F64, 1, CONST_F0);
    code_ptr += emit_reg_addr(code_ptr, OPC_ST_GLB_F64, 1, GLB_F0);
    *code_ptr++ = OPC_RET;

    assert(code_ptr - code < code_size, "seg fault: max code size exceeded!");
    return code_ptr - code;
}

void print_globals(const HeapLayout *heap) {
    const byte_t *global_segment = heap->memory + heap->const_segment_size;
    printf("globals:\n");
    printf("  i0 = %d\n", get_int(global_segment, GLB_I0));
    printf("  i1 = %d\n", get_int(global_segment, GLB_I1));
    printf("  f0 = %lf\n", get_float(global_segment, GLB_F0));
}