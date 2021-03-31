//
// Created by smackem on 27.03.21.
//

#ifndef ZLN_CALLSTACK_H
#define ZLN_CALLSTACK_H

#include <assert.h>
#include <memory.h>
#include "types.h"

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

    /// the return type of the function
    Type ret_type;

    /// zero-terminated string containing the function name. when contained in the constant segment,
    /// the length of the string may be variable.
    char name[32];
} FunctionMeta;

#define FUNCTION_META_MIN_SIZE 52

/**
 * Defines a register containing either an integer, a reference or a float.
 */
typedef union register_union {
    int32_t i32;
    addr_t ref;
    double f64;
    intptr_t ptr;
} Register;

/**
 * Describes a stack frame
 */
typedef struct stack_frame {
    /// pointer to meta information in const segment
    const FunctionMeta *meta;

    /// the index of the register in the caller stack frame
    /// to store the function's return value
    addr_t r_ret_val;

    /// the pc to return to, relative to base_pc of previous stack frame
    addr_t ret_pc;

    /// the function registers
    Register *registers;
} StackFrame;

/**
 * Describes the program's call stack
 */
typedef struct call_stack {
    /// the address of the first register bank
    Register *register_buf;

    /// the address of the first stack frame
    StackFrame *stack_frame_buf;

    /// the maximum stack depth
    size_t max_stack_depth;

    /// the number of registers per stack frame
    size_t register_count;

    /// the current stack frame
    StackFrame *top;
} CallStack;

/**
 * Initializes a call_stack instance
 *
 * @param call_stack
 *      This call stack
 *
 * @param register_buf
 *      the address of the first register bank
 *
 * @param stack_frame_buf
 *      the address of the first stack frame
 *
 * @param max_stack_depth
 *      the maximum stack depth
 *
 * @param register_count
 *      the number of registers per stack frame
 *
 * @param entry_point
 *      meta info on the entry point function
 */
void init_call_stack(CallStack *call_stack,
                     Register *register_buf,
                     StackFrame *stack_frame_buf,
                     size_t max_stack_depth,
                     size_t register_count,
                     const FunctionMeta *entry_point);

/**
 * Pushes a new stack frame
 *
 * @param call_stack
 *      This call stack
 *
 * @param function
 *      The called function
 *
 * @param r_ret_val
 *      The register on the current stack frame that receives the called function's return value
 *
 * @param ret_pc
 *      The pc to return to when this function returns
 *
 * @return the new top stack frame
 */
StackFrame *push_stack_frame(CallStack *call_stack,
                             const FunctionMeta *function,
                             byte_t r_ret_val,
                             addr_t ret_pc);

/**
 * Pops the current top stack frame from the call stack

 * @param call_stack
 *      This call stack
 *
 * @return The popped stack frame
 */
StackFrame *pop_stack_frame(CallStack *call_stack);

/**
 * @return the current stack depth
 */
size_t current_stack_depth(const CallStack *call_stack);

#endif //ZLN_CALLSTACK_H
