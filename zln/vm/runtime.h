//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_RUNTIME_H
#define ZLN_RUNTIME_H

#include "cpu.h"

void invoke(int32_t function_id, Cpu *cpu, Register *result, const Register *first_arg);

#endif //ZLN_RUNTIME_H
