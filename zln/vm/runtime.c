//
// Created by smackem on 20.03.21.
//

#include <util.h>
#include "runtime.h"

// ------------------------------------ local tools
//

static void store(byte_t *memory, addr_t addr, const Register *reg, Type type) {
    switch (type) {
        case TYPE_Int32:
            set_int(memory, addr, reg->i32);
            break;
        case TYPE_Ref:
        case TYPE_String:
            set_addr(memory, addr, reg->ref);
            break;
        case TYPE_Unsigned8:
            set_byte(memory, addr, reg->i32 & 0xff);
            break;
        case TYPE_Float64:
            set_float(memory, addr, reg->f64);
            break;
        case TYPE_NativePtr:
            set_ptr(memory, addr, reg->ptr);
            break;
        default:
            assert_that(false, "unsupported type %d", type);
            break;
    }
}

static void load(const byte_t *memory, addr_t addr, Register *reg, Type type) {
    switch (type) {
        case TYPE_Int32:
            reg->i32 = get_int(memory, addr);
            break;
        case TYPE_Ref:
        case TYPE_String:
            reg->ref = get_addr(memory, addr);
            break;
        case TYPE_Unsigned8:
            reg->i32 = get_byte(memory, addr);
            break;
        case TYPE_Float64:
            reg->f64 = get_float(memory, addr);
            break;
        case TYPE_NativePtr:
            reg->ptr = get_ptr(memory, addr);
            break;
        default:
            assert_that(false, "unsupported type %d", type);
            break;
    }
}

// ------------------------------------ built in functions
//

typedef void (*invocation_t)(Cpu *cpu, Register *result, const Register *first_arg);

static void array_size(Cpu *cpu, Register *result, const Register *first_arg) {
    const HeapEntry *self = get_heap_entry(&cpu->heap, first_arg->ref);
    uint32_t element_size = sizeof_type(self->header);
    result->i32 = (int32_t) (self->data_size / element_size);
}

static void array_copy(Cpu *cpu, Register *result, const Register *first_arg) {
    const HeapEntry *self = get_heap_entry(&cpu->heap, first_arg[0].ref);
    int32_t from = first_arg[1].i32;
    int32_t copy_size = first_arg[2].i32;
    assert_that(copy_size > 0, "copy size must be >= 0");
    uint32_t element_size = sizeof_type(self->header);
    trace("from=%d, copy_size=%d\n", from, copy_size);
    uint32_t self_size = self->data_size / element_size;
    uint32_t copy_count = min(copy_size, self_size - from);
    result->ref = alloc_array(&cpu->heap, self->header, copy_size);
    HeapEntry *copy_entry = get_heap_entry(&cpu->heap, result->ref);
    memcpy(copy_entry->data, &self->data[from * element_size], copy_count * element_size);
}

// see ListType.java
#define LIST_FIELD_SIZE 0
#define LIST_FIELD_ARRAY 4

static void list_size(Cpu *cpu, Register *result, const Register *first_arg) {
    addr_t size_addr = get_field_addr(&cpu->heap, first_arg->ref, LIST_FIELD_SIZE);
    result->i32 = get_int(cpu->heap.memory, size_addr);
}

static void list_capacity(Cpu *cpu, Register *result, const Register *first_arg) {
    addr_t array_field_addr = get_field_addr(&cpu->heap, first_arg->ref, LIST_FIELD_ARRAY);
    addr_t array_addr = get_addr(cpu->heap.memory, array_field_addr);
    const HeapEntry *array = get_heap_entry(&cpu->heap, array_addr);
    uint32_t element_size = sizeof_type(array->header);
    result->i32 = (int32_t) (array->data_size / element_size);
}

static void list_add(Cpu *cpu, Register *result, const Register *first_arg) {
    addr_t array_field_addr = get_field_addr(&cpu->heap, first_arg->ref, LIST_FIELD_ARRAY);
    addr_t array_addr = get_addr(cpu->heap.memory, array_field_addr);
    addr_t size_addr = get_field_addr(&cpu->heap, first_arg->ref, LIST_FIELD_SIZE);
    int32_t size = get_int(cpu->heap.memory, size_addr);
    const HeapEntry *array = get_heap_entry(&cpu->heap, array_addr);
    uint32_t element_size = sizeof_type(array->header);
    int32_t capacity = (int32_t) (array->data_size / element_size);
    addr_t elem_addr = get_field_addr(&cpu->heap, array_addr, size * element_size);
    if (size >= capacity - 1) {
        trace("GROW size=%d capacity=%d\n", size, capacity);
        remove_ref(&cpu->heap, array_addr);
        addr_t new_array_addr = alloc_array(&cpu->heap, array->header, capacity + 16);
        memcpy(get_heap_entry(&cpu->heap, new_array_addr)->data, array->data, size * element_size);
        set_addr(cpu->heap.memory, array_field_addr, new_array_addr);
        elem_addr = get_field_addr(&cpu->heap, new_array_addr, size * element_size);
    }
    store(cpu->heap.memory, elem_addr, &first_arg[1], array->header);
    set_int(cpu->heap.memory, size_addr, size + 1);
}

static void list_remove(Cpu *cpu, Register *result, const Register *first_arg) {
}

static void print(Cpu *cpu, Register *result, const Register *first_arg) {
}

static void string_length(Cpu *cpu, Register *result, const Register *first_arg) {
    const HeapEntry *self = get_heap_entry(&cpu->heap, first_arg->ref);
    assert(self->header == TYPE_Unsigned8);
    result->i32 = (int32_t) (self->data_size - 1); // - 1 for terminating zero
}

static invocation_t invocations[] = {
        NULL,
        array_size,     //BIF_ArrayLength     = 1,
        array_copy,     //BIF_ArrayCopy       = 2,
        NULL,           // 3
        NULL,           // 4
        NULL,           // 5
        NULL,           // 6
        NULL,           // 7
        NULL,           // 8
        NULL,           // 9
        list_size,      //BIF_ListSize        = 10,
        list_capacity,  //BIF_ListCapacity    = 11,
        list_add,       //BIF_ListAdd         = 12,
        list_remove,    //BIF_ListRemove      = 13,
        NULL,           // 14
        NULL,           // 15
        NULL,           // 16
        NULL,           // 17
        NULL,           // 18
        NULL,           // 19
        print,          //BIF_Print           = 20,
        NULL,           // 21
        NULL,           // 22
        NULL,           // 23
        NULL,           // 24
        NULL,           // 25
        NULL,           // 26
        NULL,           // 27
        NULL,           // 28
        NULL,           // 29
        string_length,  //BIF_StringLength    = 30,
};

void invoke(BuiltInFunction function_id, Cpu *cpu, Register *result, const Register *first_arg) {
    assert(function_id > 0);
    assert(function_id < sizeof(invocations) / sizeof(invocations[0]));
    assert(cpu != NULL);
    assert(result != NULL);
    assert(first_arg != NULL);
    invocations[function_id](cpu, result, first_arg);
}

int compare_strings(const Cpu *cpu, addr_t a, addr_t b) {
    if (a == 0) {
        return b == 0 ? 0 : -1;
    }
    if (b == 0) {
        return 1;
    }
    const HeapEntry *entry_a = get_heap_entry(&cpu->heap, a);
    const HeapEntry *entry_b = get_heap_entry(&cpu->heap, b);
    assert(entry_a->header == TYPE_Unsigned8);
    assert(entry_b->header == TYPE_Unsigned8);
    return strcmp((const char *) entry_a->data, (const char *) entry_b->data);
}

addr_t concat_strings(Cpu *cpu, addr_t left, addr_t right) {
    const HeapEntry *left_entry = get_heap_entry(&cpu->heap, left);
    const HeapEntry *right_entry = get_heap_entry(&cpu->heap, right);
    uint32_t size = left_entry->data_size + right_entry->data_size - 1;
    assert(left_entry->header == TYPE_Unsigned8);
    assert(right_entry->header == TYPE_Unsigned8);
    addr_t dest_addr = alloc_array(&cpu->heap, TYPE_Unsigned8, size);
    HeapEntry *dest_entry = get_heap_entry(&cpu->heap, dest_addr);
    memcpy(dest_entry->data, left_entry->data, left_entry->data_size);
    memcpy(&dest_entry->data[left_entry->data_size - 1], right_entry->data, right_entry->data_size);
    return dest_addr;
}
