//
// Created by Philip Boger on 22.03.21.
//

#ifndef ZLN_CPU_H
#define ZLN_CPU_H

#include <util.h>
#include "opcode.h"
#include "types.h"

/**
 * Holds an instruction with opcode and arguments.
 * args is just a placeholder, the address of which may be used
 * to access the variable arguments (which depend on the opcode).
 */
typedef struct instruction {
    /// the instruction opcode
    OpCode opc;

    /**
     * dummy array to address arguments.
     * arguments are either REG (single byte containing a register index)
     * or INT (32bit)
     */
    byte_t args[1];
} Instruction;

#define INSTRUCTION_MIN_SIZE 2

/**
 * Holds information about a function, stored in the const segment
 */
typedef struct function_meta {
    /// the base instruction address of the module that defines the function
    addr_t base_pc;

    /// the offset of the function's first instruction within the module
    addr_t pc;

    /// the number of local variables of the function
    int local_count;

    /// the number of arguments accepted by the function
    int arg_count;
} FunctionMeta;

#define FUNCTION_META_SIZE 16

/**
 * Holds information about a type, stored in the const segment
 */
typedef struct type_meta {
    /// the size of the type in bytes
    addr_t size;

    /// the name of the type as a zero-terminated string
    char name[1]; // zero-terminated string
} TypeMeta;

/**
 * Describes a stack frame
 */
typedef struct stack_frame {
    /// pointer to meta information in const segment
    const FunctionMeta *meta;

    /// the index of the register to store the function's return value
    addr_t ret_register_index;

    /// the base pc of the caller module
    addr_t ret_base_pc;

    /// the pc to return to
    addr_t ret_pc;

    /// the number of function arguments
    int arg_count;

    /// the number of locals
    int local_count;
} StackFrame;

/**
 * Defines a register containing either an integer, a reference or a float.
 */
typedef union register_union {
    int32_t i32;
    addr_t ref;
    double f64;
} Register;

/**
 * Describes the heap layout.
 * The heap keeps all constants, globals and dynamically allocated objects.
 * The const segment comes first, then the globals segment and the remaining
 * space can be used to allocate memory at runtime.
 */
typedef struct heap_layout {
    /// the heap base address
    byte_t *memory;

    /// the total size of the heap
    addr_t total_size;

    /// the size of the constant segment. also the index of the first byte in the global segment.
    addr_t const_segment_size;

    /// the size of the global segment.
    addr_t global_segment_size;
} HeapLayout;

/**
 * Holds runtime parameters like number of base_registers and maximum stack depth.
 */
typedef struct runtime_config {
    /// the number of base_registers per stack frame
    int register_count;

    /// the maximum stack depth (number of stack frames)
    int max_stack_depth;
} RuntimeConfig;

/**
 * Executes the given program.
 *
 * @param code
 *      The code segment that holds the instructions emitted for all modules.
 *
 * @param base_pc
 *      The address of the first instruction of the startup module (the module that contains
 *      the entry point).
 *
 * @param pc
 *      The instruction offset based on @a base_pc where execution starts (the offset of
 *      the first instruction of the entry point)
 *
 * @param heap
 *      The heap layout.
 *
 * @param config
 *      Runtime parameters.
 */
void execute(const byte_t *code,
             addr_t base_pc,
             addr_t pc,
             const HeapLayout *heap_layout,
             const RuntimeConfig *config);

#endif //ZLN_CPU_H
