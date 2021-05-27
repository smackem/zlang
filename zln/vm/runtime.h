//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_RUNTIME_H
#define ZLN_RUNTIME_H

#include "cpu.h"

typedef enum built_in_function {
    BIF_ArraySize       = 1,
    BIF_ArrayCopy       = 2,
    BIF_ArrayWrap       = 3,
    BIF_ListSize        = 10,
    BIF_ListCapacity    = 11,
    BIF_ListAdd         = 12,
    BIF_ListRemove      = 13,
    BIF_ListSet         = 14,
    BIF_ListGet         = 15,
    BIF_Print           = 20,
    BIF_StringLength    = 30,
} BuiltInFunction;

void invoke(BuiltInFunction function_id, Cpu *cpu, Register *result, const Register *first_arg);

int compare_strings(const Cpu *cpu, addr_t a, addr_t b);

#endif //ZLN_RUNTIME_H
