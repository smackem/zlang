//
// Created by smackem on 27.03.21.
//

#include "callstack.h"

void init_call_stack(CallStack *call_stack,
                     Register *register_buf,
                     StackFrame *stack_frame_buf,
                     size_t max_stack_depth,
                     size_t register_count,
                     const FunctionMeta *entry_point) {
    assert(call_stack != NULL);
    assert(register_buf != NULL);
    assert(stack_frame_buf != NULL);
    assert(max_stack_depth > 0);
    assert(register_count > 0);
    call_stack->register_buf = register_buf;
    call_stack->stack_frame_buf = stack_frame_buf;
    call_stack->max_stack_depth = max_stack_depth;
    call_stack->register_count = register_count;
    call_stack->top = stack_frame_buf;
    StackFrame *top = call_stack->top;
    bzero(top, sizeof(StackFrame));
    top->registers = register_buf;
    top->meta = entry_point;
}

StackFrame *push_stack_frame(CallStack *call_stack,
                             const FunctionMeta *function,
                             addr_t ret_register_index,
                             addr_t ret_base_pc,
                             addr_t ret_pc) {
    assert(call_stack != NULL);
    assert(function != NULL);
    assert(call_stack->top != NULL);
    assert_that(current_stack_depth(call_stack) < call_stack->max_stack_depth, "exceeding max stack depth");
    StackFrame *old_top = call_stack->top++;
    StackFrame *top = call_stack->top;
    top->meta = function;
    top->registers = old_top->registers + call_stack->register_count;
    top->ret_register_index = ret_register_index;
    top->ret_base_pc = ret_base_pc;
    top->ret_pc = ret_pc;
    return top;
}

inline size_t current_stack_depth(const CallStack *call_stack) {
    return call_stack->top - call_stack->stack_frame_buf;
}