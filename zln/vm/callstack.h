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
} FunctionMeta;

#define FUNCTION_META_SIZE 16

/**
 * Defines a register containing either an integer, a reference or a float.
 */
typedef union register_union {
    int32_t i32;
    addr_t ref;
    double f64;
} Register;

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

    /// the function registers
    Register *registers;
} StackFrame;

typedef struct call_stack {
    Register *register_buf;
    StackFrame *stack_frame_buf;
    size_t max_stack_depth;
    size_t register_count;
    StackFrame *top;
} CallStack;

void init_call_stack(CallStack *call_stack,
                     Register *register_buf,
                     StackFrame *stack_frame_buf,
                     size_t max_stack_depth,
                     size_t register_count,
                     const FunctionMeta *entry_point);

StackFrame *push_stack_frame(CallStack *call_stack,
                             const FunctionMeta *function,
                             addr_t ret_register_index,
                             addr_t ret_base_pc,
                             addr_t ret_pc);

size_t current_stack_depth(const CallStack *call_stack);

#endif //ZLN_CALLSTACK_H
