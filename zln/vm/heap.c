//
// Created by smackem on 26.03.21.
//

#include <assert.h>
#include <memory.h>
#include <util.h>
#include "heap.h"

static inline size_t unallocated_byte_count(Heap *heap) {
    return heap->size - heap->tail;
}

static inline addr_t alloc_chunk(Heap *heap, size_t data_size, addr_t header) {
    size_t entry_size = data_size + HEAP_ENTRY_HEADER_SIZE;
    size_t free_size = unallocated_byte_count(heap);
    assert_that(entry_size <= free_size, "out of memory");
    addr_t entry_addr = heap->tail;
    HeapEntry *entry = (HeapEntry *) &heap->memory[entry_addr];
    entry->header = header;
    entry->ref_count = 1;
    entry->data_size = data_size;
    bzero(entry->data, data_size);
    heap->tail += entry_size;
    return entry_addr;
}

static inline size_t heap_entry_data_size(const Heap *heap, const HeapEntry *heap_entry) {
#ifndef NDEBUG
    if ((heap_entry->header & HEAP_ENTRY_TYPE_META_FLAG) != 0) {
        addr_t const_addr = heap_entry->header & ~HEAP_ENTRY_TYPE_META_FLAG;
        const TypeMeta *type_meta = (TypeMeta *) &heap->const_segment[const_addr];
        assert_equal(sizeof_instance(type_meta), heap_entry->data_size, "TypeMeta data_size");
    }
#endif
    return heap_entry->data_size;
}

const TypeMeta *instance_type(const Heap *heap, const HeapEntry *entry) {
    if ((entry->header & HEAP_ENTRY_TYPE_META_FLAG) == 0) {
        return NULL;
    }
    addr_t const_addr = entry->header & ~HEAP_ENTRY_TYPE_META_FLAG;
    return (TypeMeta *) &heap->const_segment[const_addr];
}

size_t sizeof_instance(const TypeMeta *type) {
    size_t size = 0;
    const Type *field_type_ptr = type->field_types;
    for ( ; *field_type_ptr != TYPE_Void; field_type_ptr++) {
        size += sizeof_type(*field_type_ptr);
    }
    return size;
}

#define HEAP_RESERVED_BYTES 0x10

void init_heap(Heap *heap, byte_t *memory, size_t size, const byte_t *const_segment) {
    assert(heap != NULL);
    assert(memory != NULL);
    assert(size > HEAP_RESERVED_BYTES);
    assert(const_segment != NULL);
    heap->memory = memory;
    heap->size = size;
    heap->tail = HEAP_RESERVED_BYTES; // reserve first X bytes => 0 (nil) is not a valid heap address
    heap->const_segment = const_segment;
}

addr_t alloc_array(Heap *heap, Type element_type, size_t size) {
    assert(heap != NULL);
    assert(size >= 0);
    size_t data_size = sizeof_type(element_type) * size;
    return alloc_chunk(heap, data_size, element_type);
}

addr_t alloc_obj(Heap *heap, addr_t type_meta_const_addr) {
    assert(heap != NULL);
    const TypeMeta *type_meta = (TypeMeta *) &heap->const_segment[type_meta_const_addr];
    return alloc_chunk(heap, sizeof_instance(type_meta), type_meta_const_addr | HEAP_ENTRY_TYPE_META_FLAG);
}

inline addr_t get_field_addr(const Heap *heap, addr_t entry_addr, addr_t offset) {
    const HeapEntry *entry = (HeapEntry *) &heap->memory[entry_addr];
    assert_that(offset < heap_entry_data_size(heap, entry), "field address out of bounds");
    return entry_addr + offset + HEAP_ENTRY_HEADER_SIZE;
}

uint32_t add_ref(Heap *heap, addr_t heap_addr) {
    HeapEntry *entry = (HeapEntry *) &heap->memory[heap_addr];
    return ++(entry->ref_count);
}

uint32_t remove_ref(Heap *heap, addr_t heap_addr) {
    HeapEntry *entry = (HeapEntry *) &heap->memory[heap_addr];
    entry->ref_count--;
    if (entry->ref_count > 0) {
        return entry->ref_count;
    }
    // reference count is zero => dispose instance
    // call remove_ref for all instances referenced by this instance
    // a) array of ref
    if (entry->header == TYPE_Ref) {
        size_t elem_count = entry->data_size / sizeof_type(TYPE_Ref);
        addr_t *addr_ptr = (addr_t *) entry->data;
        for ( ; elem_count > 0; elem_count--, addr_ptr++) {
            if (*addr_ptr != 0) {
                remove_ref(heap, *addr_ptr);
            }
        }
        return 0;
    }
    // b) user type
    const TypeMeta *type = instance_type(heap, entry);
    if (type != NULL) {
        const Type *field_type_ptr = type->field_types;
        byte_t *field_data_ptr = entry->data;
        for ( ; *field_type_ptr != TYPE_Void; field_type_ptr++) {
            if (*field_type_ptr == TYPE_Ref) {
                addr_t addr = get_addr(field_data_ptr, 0);
                if (addr != 0) {
                    remove_ref(heap, addr);
                }
            }
            field_data_ptr += sizeof_type(*field_type_ptr);
        }
    }
    return 0;
}
