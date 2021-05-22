//
// Created by smackem on 20.03.21.
//

#include <util.h>
#include "runtime.h"

typedef void (*invocation_t)(Cpu *cpu, Register *result, const Register *first_arg);

static void array_size(Cpu *cpu, Register *result, const Register *first_arg) {
    const HeapEntry *entry = get_heap_entry(&cpu->heap, first_arg->ref);
    uint32_t element_size = sizeof_type(entry->header);
    result->i32 = (int32_t) (entry->data_size / element_size);
}

static void array_copy(Cpu *cpu, Register *result, const Register *first_arg) {
    const HeapEntry *self_entry = get_heap_entry(&cpu->heap, first_arg[0].ref);
    int32_t from = first_arg[1].i32;
    int32_t copy_size = first_arg[2].i32;
    assert_that(copy_size > 0, "copy size must be >= 0");
    uint32_t element_size = sizeof_type(self_entry->header);
    trace("from=%d, copy_size=%d\n", from, copy_size);
    uint32_t self_size = self_entry->data_size / element_size;
    uint32_t copy_count = min(copy_size, self_size - from);
    result->ref = alloc_array(&cpu->heap, self_entry->header, copy_size);
    HeapEntry *copy_entry = get_heap_entry(&cpu->heap, result->ref);
    memcpy(copy_entry->data, &self_entry->data[from * element_size], copy_count * element_size);
}

static invocation_t invocations[] = {
        NULL,
        array_size,
        array_copy,
};

void invoke(BuiltInFunction function_id, Cpu *cpu, Register *result, const Register *first_arg) {
    assert(function_id > 0);
    assert(function_id < sizeof(invocations) / sizeof(invocations[0]));
    assert(cpu != NULL);
    assert(result != NULL);
    assert(first_arg != NULL);
    invocations[function_id](cpu, result, first_arg);
}
