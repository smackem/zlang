//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_RUNTIME_H
#define ZLN_RUNTIME_H

#include "cpu.h"

typedef enum built_in_function {
    BIF_ArrayLength = 1,
    BIF_ArrayCopy,
} BuiltInFunction;

void invoke(BuiltInFunction function_id, Cpu *cpu, Register *result, const Register *first_arg);

#endif //ZLN_RUNTIME_H
