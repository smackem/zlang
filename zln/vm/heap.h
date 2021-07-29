//
// Created by smackem on 26.03.21.
//

#ifndef ZLN_HEAP_H
#define ZLN_HEAP_H

#include "types.h"
#include "callstack.h"

typedef struct vtable_entry {
    /// const address of a virtual function (VirtualFunctionMeta)
    addr_t virtual_function;

    /// const address of the implementing function (FunctionMeta)
    addr_t impl_function;
} VTableEntry;

#define VTABLE_ENTRY_SIZE 8

/**
 * Holds information about a type, stored in the const segment.
 * all offsets are relative to <c>TypeMeta.data</c>:
 * to access a member, use this expression: <c>&type_meta->data[type_meta->name_offset]</c>
 */
typedef struct type_meta {
    /// data offset of the name of the type as a zero-terminated string
    addr_t name_offset;

    /// data offset of the addresses of implemented interfaces in const segment.
    /// implemented interfaces is a zero-terminated list of <c>addr_t</c>s
    addr_t implemented_interfaces_offset;

    /// data offset of the vtable.
    /// the vtable is a zero-terminated list of <c>VTableEntry</c> structs. the last struct has all fields zeroed.
    addr_t vtable_offset;

    /// data offset of a zero-terminated list of field types.
    /// field types is a list of <c>Type</c>s, terminated by a <c>TYPE_Void</c>
    addr_t field_types_offset;

    /// the data chunk
    byte_t data[4];
} TypeMeta;

#define TYPE_META_MIN_SIZE 20

/**
 * Holds a heap entry (dynamically allocated, typed data chunk - e.g. an array, struct or union)
 */
typedef struct heap_entry {
    /// if HEAP_ENTRY_TYPE_META_FLAG is set, lower 31 bits denote the address of a TypeMeta in the const segment (for composite types)
    /// if HEAP_ENTRY_TYPE_META_FLAG is clear, the entry contains an array and the lower 31 bits denote the element type (see ::Type)
    addr_t header;

    /// number of references to this entry. if 0, the entry can be cleared.
    uint32_t ref_count;

    /// the data size in bytes
    uint32_t data_size;

    /// number of allocated bytes. >= data_size. address of next entry is entry->data[entry->alloc_size].
    uint32_t alloc_size;

    /// first data byte. actual number of bytes can be deducted from header.
    /// padded in struct to 8 bytes
    byte_t data[4];
} HeapEntry;

#define HEAP_ENTRY_TYPE_META_FLAG 0x80000000
#define HEAP_ENTRY_MIN_SIZE 20
#define HEAP_ENTRY_HEADER_SIZE 16

/**
 * The heap - memory segment to hold dynamically allocated objects
 */
typedef struct heap {
    /// the heap base address
    byte_t *memory;

    /// the data_size of the heap
    uint32_t size;

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
void init_heap(Heap *heap, byte_t *memory, uint32_t size, const byte_t *const_segment);

/**
 * Allocates a new array on the heap with an initial reference count of 0.
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
addr_t alloc_array(Heap *heap, Type element_type, uint32_t size);

/**
 * Allocates a new object on the heap with an initial reference count of 0.
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
 * Allocates a new string on the heap (array of element type u8) with an initial reference count of 0.
 *
 * @param heap
 *      This heap.
 *
 * @param const_addr
 *      The const address of the raw zero-terminated string in the constant segment.
 *
 * @return The heap address of the new entry or 0 if out of memory.
 */
addr_t alloc_str(Heap *heap, addr_t const_addr);

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
 * Gets the heap entry at the specified address.
 *
 * @param heap
 *      This heap.
 *
 * @param heap_addr
 *      The address of the heap entry (array or object) to test.
 *
 * @return The heap entry at the specified address;
 */
HeapEntry *get_heap_entry(const Heap *heap, addr_t heap_addr);

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

/**
 * @return the data_size of an instance of the specified TypeMeta
 */
uint32_t sizeof_instance(const TypeMeta *type);

/**
 * @return a pointer to the TypeMeta describing the heap entry's type or NULL if the
 *      entry is not an instance of a user type.
 */
const TypeMeta *instance_type(const Heap * heap, const HeapEntry *entry);

/**
 * implements dynamic dispatch by looking up the implementation function for virtual_function_addr
 * in the vtable of the instance at heap_addr.
 *
 * @param heap
 *      This heap.
 *
 * @param heap_addr
 *      The address of the object that implements the virtual function.
 *
 * @param virtual_function_addr
 *      The address of a VirtualFunctionMeta object in the const segment.
 *
 * @return A pointer to the FunctionMeta struct that describes the implementation function.
 */
const FunctionMeta *get_impl_function(const Heap *heap, addr_t heap_addr, addr_t virtual_function_addr);

/**
 * checks if the object at {@code heap_addr} implements the user type at {@code type_addr}
 * in the const segment. either the object is of the specified type or the specified type
 * is an interface implemented by the type of the object.
 *
 * @param heap
 *      This heap.
 *
 * @param heap_addr
 *      The address of the object to check.
 *
 * @param type_addr
 *      The address of the type to test for, relative to the const segment.
 *
 * @return {@code true} if the object implements the given type, otherwise {@code false}.
 */
bool check_type(const Heap *heap, addr_t heap_addr, addr_t type_addr);

/**
 * frees memory allocated by unreferenced entries.
 *
 * @param heap
 *      This heap.
 */
void collect_heap_memory(Heap *heap);

#endif //ZLN_HEAP_H
