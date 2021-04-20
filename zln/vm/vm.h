//
// Created by smackem on 24.03.21.
//

#ifndef ZLN_VM_H
#define ZLN_VM_H

#include <stdio.h>
#include "cpu.h"

/**
 * Gets the name of the given op code.
 */
const char *opcode_name(OpCode opc);

/**
 * Gets the name of the given type.
 */
const char *type_name(Type type);

/**
 * Prints the given instruction to the specified stream.
 */
uint32_t print_instruction(FILE *f, const Instruction *instr);

/**
 * Prints all instructions that make up the given code to the specified stream.
 */
void print_code(FILE *f, const byte_t *code, uint32_t code_size);

/**
 * Prints all register contents to the specified stream.
 */
void print_registers(FILE *f, const Register *registers, int count);

/**
 * Prints the current CPU state. Can be used as CPU debug callback.
 */
void dump_cpu(addr_t pc,
              const Instruction *instr,
              uint32_t stack_depth,
              const StackFrame *stack_frame,
              const Register *registers,
              uint32_t register_count);

#endif //ZLN_VM_H
