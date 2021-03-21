//
// Created by Philip Boger on 20.03.21.
//

#ifndef ZLN_OPCODE_H
#define ZLN_OPCODE_H

typedef enum opcode {
    NOP = 0,
    LD_LOC,
    LD_GLB,
    LD_FLD,
    ST_LOC,
    ST_GLB,
    ST_FLD,
    PUSH,
    POP,
    ADD,
    SUB,
    MUL,
    DIV,
    AND,
    OR,
    BR_FALSE,
    BR,
    RET,
    CONVERT,
} OpCode;

#endif //ZLN_OPCODE_H
