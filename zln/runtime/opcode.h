//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_OPCODE_H
#define ZLN_OPCODE_H

#include "types.h"

enum op_code {
    /**
     * nop():
     */
    OPC_NOP = 0,

    /**
     * load_global(REG r_target, INT glb_offset):
     *      r_target <- *heap_offset
     * glb_offset is relative to global base
     */
    OPC_LD_GLB_I32,
    OPC_LD_GLB_F64,
    OPC_LD_GLB_U8,
    OPC_LD_GLB_REF,

    /**
     * load_field(REG r_target, REG r_obj_addr, INT field_offset):
     *      r_target <- *(r_obj_addr + field_offset)
     * r_obj_addr is relative to heap base
     */
    OPC_LD_FLD_I32,
    OPC_LD_FLD_F64,
    OPC_LD_FLD_U8,
    OPC_LD_FLD_REF,

    /**
     * store_global(REG r_source, INT glb_offset):
     *      r_source -> *heap_offset
     * glb_offset is relative to global base
     */
    OPC_ST_GLB_I32,
    OPC_ST_GLB_F64,
    OPC_ST_GLB_U8,
    OPC_ST_GLB_REF,

    /**
     * store_field(REG r_source, REG r_obj_addr, INT field_offset):
     *      r_source -> *(r_obj_addr + field_offset)
     * r_obj_addr is relative to heap base
     */
    OPC_ST_FLD_I32,
    OPC_ST_FLD_F64,
    OPC_ST_FLD_U8,
    OPC_ST_FLD_REF,

    /**
     * load_immediate_constant(REG r_target, INT value):
     *      r_target <- value
     */
    OPC_LD_C_I32,
    OPC_LD_C_REF,
    /**
     * load_constant(REG r_target, INT const_offset):
     *      r_target <- *const_offset
     * const_offset is relative to const base
     */
    OPC_LD_C_F64,

    /**
     * add(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left + r_right
     */
    OPC_ADD_I32,
    OPC_ADD_F64,
    OPC_ADD_U8,
    OPC_ADD_STR,

    /**
     * sub(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left - r_right
     */
    OPC_SUB_I32,
    OPC_SUB_F64,
    OPC_SUB_U8,

    /**
     * mul(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left * r_right
     */
    OPC_MUL_I32,
    OPC_MUL_F64,
    OPC_MUL_U8,

    /**
     * div(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left / r_right
     */
    OPC_DIV_I32,
    OPC_DIV_F64,
    OPC_DIV_U8,

    /**
     * and(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left and r_right
     */
    OPC_AND,

    /**
     * or(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left or r_right
     */
    OPC_OR,

    /**
     * branch_if_false(REG r_source, INT code_offset):
     *      if r_source != 0:
     *          pc <- code_offset
     * code_offset is relative to base_pc
     */
    OPC_BR_FALSE,

    /**
     * branch(INT code_offset):
     *      pc <- code_offset
     * code_offset is relative to base_pc
     */
    OPC_BR,

    /**
     * call(REG r_target, INT heap_offset):
     *      (look at function_addr at heap_offset)
     *      push stack_frame(#r_target, base_pc, pc, (*heap_offset).arg_count, (*heap_offset).local_count)
     *      push base_registers
     *      base_pc <- (*heap_offset).base_addr
     *      pc <- (*heap_offset).offset
     */
    OPC_CALL,

    /**
     * return():
     *      pop stack_frame sf
     *      base_pc <- sf.base_pc
     *      pc <- sf.pc + 1
     *      save r0 to ret_val
     *      pop base_registers
     *      sf.r_target <- ret_val
     */
    OPC_RET,

    /**
     * convert(REG r_target, REG r_source, TYPE target_type):
     *      r_target <- r_source converted to target_type (see ::Type)
     */
    OPC_CONV_I32,
    OPC_CONV_F64,
    OPC_CONV_U8,
    OPC_CONV_STR,
    OPC_CONV_REF,

    /**
     * move(REG r_target, REG r_source):
     *      r_target <- r_source
     */
    OPC_MOV,

    /**
     * new_object(REG r_target, INT heap_offset):
     *      (look at type_meta at heap_offset)
     *      allocate memory on heap for new instance of type_meta
     *      r_target <- addr of new memory block (TYPE_REF)
     * heap_offset is relative to heap base
     */
    OPC_NEW_OBJ,

    /**
     * new_string(REG r_target, INT size):
     *      allocate memory on heap for size characters
     *      r_target <- addr of new memory block (TYPE_REF)
     */
    OPC_NEW_STR,

    /**
     * new_array(REG r_target, INT size):
     *      allocate memory on heap for size items
     *      r_target <- addr of new memory block (TYPE_REF)
     */
    OPC_NEW_ARR_I32,
    OPC_NEW_ARR_F64,
    OPC_NEW_ARR_U8,
    OPC_NEW_ARR_REF,
};

typedef byte_t OpCode;

#endif //ZLN_OPCODE_H
