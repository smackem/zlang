//
// Created by smackem on 26.03.21.
//

#include <assert.h>
#include <memory.h>
#include "heap.h"

static inline size_t unallocated_byte_count(Heap *heap) {
    return heap->size - heap->tail;
}

static inline addr_t alloc_chunk(Heap *heap, size_t data_size, addr_t header) {
    size_t entry_size = data_size + HEAP_ENTRY_HEADER_SIZE;
    size_t free_size = unallocated_byte_count(heap);
    if (entry_size > free_size) {
        return 0;
    }

    addr_t entry_addr = heap->tail;
    HeapEntry *entry = (HeapEntry *) &heap->memory[entry_addr];
    entry->header = header;
    entry->ref_count = 1;
    bzero(entry->data, data_size);
    heap->tail += entry_size;
    return entry_addr;
}

static size_t heap_entry_data_size(const Heap *heap, const HeapEntry *heap_entry) {
    if ((heap_entry->header & HEAP_ENTRY_TYPE_META_FLAG) == 0) {
        return heap_entry->header;
    }
    addr_t const_addr = heap_entry->header & ~HEAP_ENTRY_TYPE_META_FLAG;
    const TypeMeta *type_meta = (TypeMeta *) &heap->const_segment[const_addr];
    assert_that(type_meta->size != 0, "TypeMeta size is 0");
    return type_meta->size;
}

void init_heap(Heap *heap, byte_t *memory, size_t size, const byte_t *const_segment) {
    assert(heap != NULL);
    assert(memory != NULL);
    assert(size > 0);
    assert(const_segment != NULL);
    heap->memory = memory;
    heap->size = size;
    heap->tail = 0;
}

addr_t alloc_array(Heap *heap, Type element_type, size_t size) {
    assert(heap != NULL);
    assert(size >= 0);
    size_t data_size = sizeof_type(element_type) * size;
    return alloc_chunk(heap, data_size, data_size);
}

addr_t alloc_obj(Heap *heap, addr_t type_meta_const_addr) {
    assert(heap != NULL);
    const TypeMeta *type_meta = (TypeMeta *) &heap->const_segment[type_meta_const_addr];
    return alloc_chunk(heap, type_meta->size, type_meta->size | HEAP_ENTRY_TYPE_META_FLAG);
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
    return --(entry->ref_count);
}
