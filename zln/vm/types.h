//
// Created by Philip Boger on 20.03.21.
//

#ifndef ZLN_TYPES_H
#define ZLN_TYPES_H

typedef uint8_t byte_t;
typedef uint32_t addr_t;

enum type {
    TYPE_Int32 = 0,
    TYPE_Float64,
    TYPE_Unsigned8,
    TYPE_String,
    TYPE_Ref,
};

typedef byte_t Type;

#define get_byte(ptr, offset) *((ptr) + offset)
#define set_byte(ptr, offset, b) (*((ptr) + offset) = b)

#define get_int(ptr, offset) (*(int32_t *) ((ptr) + offset))
#define set_int(ptr, offset, i) (*(int32_t *) ((ptr) + offset) = i)

#define get_addr(ptr, offset) (*(addr_t *) ((ptr) + offset))
#define set_addr(ptr, offset, a) (*(addr_t *) ((ptr) + offset) = a)

#define get_float(ptr, offset) (*(double *) ((ptr) + offset))
#define set_float(ptr, offset, f) (*(double *) ((ptr) + offset) = f)

#define get_type(ptr, offset)  (*(Type *) ((ptr) + offset))

#endif //ZLN_TYPES_H
