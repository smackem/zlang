//
// Created by smackem on 22.03.21.
//

#include <stdio.h>
#include <vm.h>

size_t emit_reg_int(byte_t *code, OpCode opc, byte_t r, int32_t i) {
    code[0] = opc;
    code[1] = r;
    set_int(code, 2, i);
    return 6;
}

size_t emit_reg3(byte_t *code, OpCode opc, byte_t r_first, byte_t r_second, byte_t r_third) {
    code[0] = opc;
    code[1] = r_first;
    code[2] = r_second;
    code[3] = r_third;
    return 4;
}

size_t emit_reg_addr(byte_t *code, OpCode opc, byte_t r, addr_t addr) {
    code[0] = opc;
    code[1] = r;
    set_addr(code, 2, addr);
    return 6;
}

size_t emit_reg2(byte_t *code, OpCode opc, byte_t r_target, byte_t r_source) {
    code[0] = opc;
    code[1] = r_target;
    code[2] = r_source;
    return 3;
}

size_t emit_reg2_addr(byte_t *code, OpCode opc, byte_t r_first, byte_t r_second, addr_t addr) {
    code[0] = opc;
    code[1] = r_first;
    code[2] = r_second;
    set_addr(code, 3, addr);
    return 7;
}

size_t emit_addr(byte_t *code, OpCode opc, addr_t addr) {
    code[0] = opc;
    set_addr(code, 1, addr);
    return 5;
}
