//
// Created by Philip Boger on 22.03.21.
//

#ifndef ZLN_EMIT_H
#define ZLN_EMIT_H

#include <vm.h>

size_t emit_reg_int(byte_t *code, OpCode opc, byte_t r_target, int32_t i);
size_t emit_binary_op(byte_t *code, OpCode opc, byte_t r_target, byte_t r_left, byte_t r_right);
size_t emit_reg_addr(byte_t *code, OpCode opc, byte_t r_source, addr_t addr);
size_t emit_reg_reg(byte_t *code, OpCode opc, byte_t r_target, byte_t r_source);
size_t emit_conv(byte_t *code, OpCode opc, byte_t r_target, byte_t r_source, Type target_type);

#endif //ZLN_EMIT_H
