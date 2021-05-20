//
// Created by smackem on 20.03.21.
//

#include "runtime.h"

typedef void (*invocation_t)(Cpu *cpu, Register *result, const Register *first_arg);

static void array_length(Cpu *cpu, Register *result, const Register *first_arg) {
    assert(cpu != NULL);
    assert(result != NULL);
    assert(first_arg != NULL);
    const HeapEntry *entry = (HeapEntry *) &cpu->heap.memory[first_arg->ref];
    uint32_t element_size = sizeof_type(entry->header);
    result->i32 = (int32_t) (entry->data_size / element_size);
}

static void array_copy(Cpu *cpu, Register *result, const Register *first_arg) {
}

static invocation_t invocations[] = {
        NULL,
        array_length,
        array_copy,
};

void invoke(BuiltInFunction function_id, Cpu *cpu, Register *result, const Register *first_arg) {
    assert(function_id > 0);
    assert(function_id < sizeof(invocations) / sizeof(invocations[0]));
    invocations[function_id](cpu, result, first_arg);
}
