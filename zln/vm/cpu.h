//
// Created by Philip Boger on 22.03.21.
//

#ifndef ZLN_CPU_H
#define ZLN_CPU_H

#include "types.h"
#include "opcode.h"
#include "heap.h"
#include "callstack.h"

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
     * or INT (32bit).
     */
    byte_t args[1];
} Instruction;

#define INSTRUCTION_MIN_SIZE 2

/**
 * Describes the memory layout.
 * Contains all constants, globals and the heap of dynamically allocated objects.
 * The const segment comes first, then the globals segment and the remaining
 * space can be used to allocate base at runtime.
 */
typedef struct memory_layout {
    /// the address of the memory chunk to use.
    byte_t *base;

    /// the total base data_size
    addr_t total_size;

    /// the data_size of the constant segment. also the index of the first byte in the global segment.
    addr_t const_segment_size;

    /// the data_size of the global segment. the heap base is base + const_segment_size + global_segment_size
    addr_t global_segment_size;

    /// size of the register segment. must be large enough to hold register_count * max_stack_depth * sizeof(Register) bytes.
    /// (see ::RuntimeConfig and ::Register)
    addr_t register_segment_size;

    /// size of the stack frames segment. must be large enough to hold max_stack_septh * sizeof(StackFrame) bytes.
    /// (see ::RuntimeConfig and ::StackFrame)
    addr_t stack_frame_segment_size;
} MemoryLayout;

/**
 * @return the address of the const segment.
 */
const byte_t *const_segment_addr(const MemoryLayout *memory);

/**
 * @return the address of the global segment.
 */
const byte_t *global_segment_addr(const MemoryLayout *memory);

/**
 * @return the address of the heap segment.
 */
const byte_t *heap_segment_addr(const MemoryLayout *memory);

/**
 * Can be called by the cpu for each instruction.
 */
typedef void (*debug_callback_t)(addr_t pc,
        const Instruction *instr,
        uint32_t stack_depth,
        const StackFrame *stack_frame,
        const Register *registers,
        uint32_t register_count);

/**
 * Holds runtime parameters like number of base_registers and maximum stack depth.
 */
typedef struct runtime_config {
    /// the number of base_registers per stack frame
    int register_count;

    /// the maximum stack depth (number of stack frames)
    int max_stack_depth;

    /// if not NULL, this callback is invoked for each instruction BEFORE execution
    debug_callback_t debug_callback;
} RuntimeConfig;

/**
 * Internal type to hold the state of a virtual cpu
 */
typedef struct cpu {
    CallStack call_stack;
    const byte_t *const_segment;
    byte_t *global_segment;
    Heap heap;
} Cpu;

/**
 * Executes the given program.
 *
 * @param code
 *      The code segment that holds the instructions emitted for all modules.
 *
 * @param entry_point
 *      The address of the meta information on the program's entry point function
 *      (usually 'main').
 *
 * @param heap
 *      The heap layout.
 *
 * @param config
 *      Runtime parameters.
 */
void execute(const byte_t *code,
             const FunctionMeta *entry_point,
             const MemoryLayout *memory,
             const RuntimeConfig *config);

#endif //ZLN_CPU_H
