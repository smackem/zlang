//
// Created by Philip Boger on 22.03.21.
//

#ifndef ZLN_EMIT_H
#define ZLN_EMIT_H

#include <vm.h>

size_t emit_reg(byte_t *code, OpCode opc, byte_t r);
size_t emit_reg_int(byte_t *code, OpCode opc, byte_t r, int32_t i);
size_t emit_reg_addr(byte_t *code, OpCode opc, byte_t r, addr_t addr);
size_t emit_reg2(byte_t *code, OpCode opc, byte_t r_target, byte_t r_source);
size_t emit_reg2_addr(byte_t *code, OpCode opc, byte_t r_first, byte_t r_second, addr_t addr);
size_t emit_reg3(byte_t *code, OpCode opc, byte_t r_first, byte_t r_second, byte_t r_third);
size_t emit_addr(byte_t *code, OpCode opc, addr_t addr);

#endif //ZLN_EMIT_H
