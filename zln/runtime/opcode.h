//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_OPCODE_H
#define ZLN_OPCODE_H

typedef enum opCode {
    OPC_NOP = 0,
    OPC_LD_LOC,
    OPC_LD_GLB,
    OPC_LD_FLD,
    OPC_ST_LOC,
    OPC_ST_GLB,
    OPC_ST_FLD,
    OPC_PUSH,
    OPC_POP,
    OPC_ADD,
    OPC_SUB,
    OPC_MUL,
    OPC_DIV,
    OPC_AND,
    OPC_OR,
    OPC_BR_FALSE,
    OPC_BR,
    OPC_RET,
    OPC_CONVERT,
} OpCode;

#endif //ZLN_OPCODE_H
