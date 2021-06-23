//
// Created by smackem on 27.03.21.
//

#include "callstack.h"

void init_call_stack(CallStack *call_stack,
                     Register *register_buf,
                     StackFrame *stack_frame_buf,
                     uint32_t max_stack_depth,
                     uint32_t register_count,
                     const FunctionMeta *entry_point) {
    assert(call_stack != NULL);
    assert(register_buf != NULL);
    assert(stack_frame_buf != NULL);
    assert(max_stack_depth > 0);
    call_stack->register_buf = register_buf;
    call_stack->stack_frame_buf = stack_frame_buf;
    call_stack->max_stack_depth = max_stack_depth;
    call_stack->register_count = register_count;
    call_stack->top = stack_frame_buf;
    StackFrame *top = call_stack->top;
    zero_memory(top, sizeof(StackFrame));
    top->registers = register_buf;
    top->meta = entry_point;
}

StackFrame *push_stack_frame(CallStack *call_stack,
                             const FunctionMeta *function,
                             byte_t r_ret_val,
                             addr_t ret_pc) {
    assert(call_stack != NULL);
    assert(function != NULL);
    assert(call_stack->top != NULL);
    assert_that(current_stack_depth(call_stack) < call_stack->max_stack_depth, "call stack overflow");
    StackFrame *old_top = call_stack->top++;
    StackFrame *top = call_stack->top;
    top->meta = function;
    top->registers = old_top->registers + call_stack->register_count;
    top->r_ret_val = r_ret_val;
    top->ret_pc = ret_pc;
    return top;
}

StackFrame *pop_stack_frame(CallStack *call_stack) {
    assert(call_stack != NULL);
    assert_that(current_stack_depth(call_stack) > 0, "call stack underflow");
    return call_stack->top--;
}

inline uint32_t current_stack_depth(const CallStack *call_stack) {
    return call_stack->top - call_stack->stack_frame_buf + 1;
}