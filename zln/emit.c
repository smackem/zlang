//
// Created by smackem on 22.03.21.
//

#include <stdio.h>
#include <vm.h>

size_t emit_reg_int(byte_t *code, OpCode opc, byte_t r_target, int32_t i) {
    code[0] = opc;
    code[1] = r_target;
    set_int(code, 2, i);
    return 6;
}

size_t emit_binary_op(byte_t *code, OpCode opc, byte_t r_target, byte_t r_left, byte_t r_right) {
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
