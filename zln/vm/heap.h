//
// Created by smackem on 26.03.21.
//

#ifndef ZLN_HEAP_H
#define ZLN_HEAP_H

#include "types.h"

#define MAX_IMPLEMENTED_INTERFACES 8

/**
 * Holds information about a type, stored in the const segment
 */
typedef struct type_meta {
    /// the name of the type as a zero-terminated string
    char name[64]; // always 64 bytes (padded with zeroes)

    /// number of implemented interfaces
    size_t implemented_interfaces_count;

    /// addresses of implemented interfaces in const segment
    addr_t implemented_interfaces[MAX_IMPLEMENTED_INTERFACES];

    /// zero-terminated list of field types -
    Type field_types[4]; // zero-terminated (last item is TYPE_Void), 4 bytes for padding
} TypeMeta;

#define TYPE_META_MIN_SIZE 112

/**
 * @return the data_size of an instance of the specified TypeMeta
 */
size_t instance_size(const TypeMeta *type);

/**
 * Holds a heap entry (dynamically allocated, typed data chunk - e.g. an array, struct or union)
 */
typedef struct heap_entry {
    /// if HEAP_ENTRY_TYPE_META_FLAG is set, lower 31 bits denote the address of a TypeMeta in the const segment (for composite types)
    /// if HEAP_ENTRY_TYPE_META_FLAG is clear, the entry contains an array and the lower 31 bits denote the element type (see ::Type)
    addr_t header;

    /// number of references to this entry. if 0, the entry can be cleared.
    uint32_t ref_count;

    /// the data data_size in bytes of the entry
    size_t data_size;

    /// first data byte. actual number of bytes can be deducted from header.
    /// padded in struct to 8 bytes
    byte_t data[8];
} HeapEntry;

#define HEAP_ENTRY_TYPE_META_FLAG 0x80000000
#define HEAP_ENTRY_MIN_SIZE 24
#define HEAP_ENTRY_HEADER_SIZE 12

/**
 * The heap - memory segment to hold dynamically allocated objects
 */
typedef struct heap {
    /// the heap base address
    byte_t *memory;

    /// the data_size of the heap
    size_t size;

    /// the current tail (position after last heap entry)
    addr_t tail;

    /// the address of the const segment, used to look up TypeMeta information
    const byte_t *const_segment;
} Heap;

/**
 * Initialize the heap.
 *
 * @param heap
 *      The heap to initialize.
 *
 * @param memory
 *      The heap's base memory address.
 *
 * @param size
 *      The data_size of the heap.
 *
 * @param const_segment
 *      The address of the constant segment used to look up TypeMeta information.
 */
void init_heap(Heap *heap, byte_t *memory, size_t size, const byte_t *const_segment);

/**
 * Allocates a new array on the heap with an initial reference count of 1.
 *
 * @param heap
 *      This heap.
 *
 * @param element_type
 *      The type of elements the new array contains.
 *
 * @param size
 *      The number of array elements.
 *
 * @return The heap address of the new entry or 0 if out of memory.
 */
addr_t alloc_array(Heap *heap, Type element_type, size_t size);

/**
 * Allocates a new object on the heap with an initial reference count of 1.
 *
 * @param heap
 *      This heap.
 *
 * @param type_meta_const_addr
 *      The const address of the TypeMeta information in the const segment
 *      that describe the type to allocate.
 *
 * @return The heap address of the new entry or 0 if out of memory.
 */
addr_t alloc_obj(Heap *heap, addr_t type_meta_const_addr);

/**
 * Gets the address of a field/cell relative to the entry at the specified heap address.
 *
 * @param heap
 *      This heap.
 *
 * @param heap_addr
 *      The address of the heap entry (array or object) to test.
 *
 * @param offset
 *      The data offset (in bytes) of the field in question.
 *
 * @return The heap address of the specified field or array cell.
 */
addr_t get_field_addr(const Heap *heap, addr_t heap_addr, addr_t offset);

/**
 * Increments the reference count of an object stored on the heap.
 *
 * @param heap
 *      This heap.
 *
 * @param heap_addr
 *      The heap address of the entry in question.
 *
 * @return the new reference count of the modified heap entry.
 */
uint32_t add_ref(Heap *heap, addr_t heap_addr);

/**
 * Decrements the reference count of an object stored on the heap.
 *
 * @param heap
 *      This heap.
 *
 * @param heap_addr
 *      The heap address of the entry in question.
 *
 * @return the new reference count of the modified heap entry.
 */
uint32_t remove_ref(Heap *heap, addr_t heap_addr);

#endif //ZLN_HEAP_H
