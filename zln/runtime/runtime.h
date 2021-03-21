//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_RUNTIME_H
#define ZLN_RUNTIME_H

#include <stdint.h>
#include <stdlib.h>
#include "opcode.h"
#include "types.h"

typedef struct instruction {
    OpCode opCode;
    Type type;
    int32_t int32_arg;
    double float64_arg;
    intptr_t ref_arg;
} Instruction;

#endif //ZLN_RUNTIME_H
