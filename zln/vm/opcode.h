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
    OPC_Nop = 0,

    /**
     * load_global(REG r_target, INT glb_addr):
     *      r_target <- *glb_addr
     * glb_addr is relative to global base
     */
    OPC_LdGlb_i32,
    OPC_LdGlb_f64,
    OPC_LdGlb_u8,
    OPC_LdGlb_ref,

    /**
     * load_field(REG r_target, REG r_heap_addr, INT field_offset):
     *      r_target <- *(r_heap_addr + field_offset)
     * r_heap_addr is relative to heap base
     */
    OPC_LdFld_i32,
    OPC_LdFld_f64,
    OPC_LdFld_u8,
    OPC_LdFld_ref,

    /**
     * store_global(REG r_source, INT glb_addr):
     *      r_source -> *glb_addr
     * glb_addr is relative to global base
     */
    OPC_StGlb_i32,
    OPC_StGlb_f64,
    OPC_StGlb_u8,
    OPC_StGlb_ref,

    /**
     * store_field(REG r_source, REG r_heap_addr, INT field_offset):
     *      r_source -> *(r_heap_addr + field_offset)
     * r_heap_addr is relative to heap base
     */
    OPC_StFld_i32,
    OPC_StFld_f64,
    OPC_StFld_u8,
    OPC_StFld_ref,

    /**
     * load_immediate_constant(REG r_target, INT value):
     *      r_target <- value
     */
    OPC_Ldc_i32,
    OPC_Ldc_ref,
    /**
     * load_constant(REG r_target, INT const_addr):
     *      r_target <- *const_addr
     * const_addr is relative to const base
     */
    OPC_Ldc_f64,

    /**
     * add(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left + r_right
     */
    OPC_Add_i32,
    OPC_Add_f64,
    OPC_Add_u8,
    OPC_Add_str,

    /**
     * sub(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left - r_right
     */
    OPC_Sub_i32,
    OPC_Sub_f64,
    OPC_Sub_u8,

    /**
     * mul(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left * r_right
     */
    OPC_Mul_i32,
    OPC_Mul_f64,
    OPC_Mul_u8,

    /**
     * div(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left / r_right
     */
    OPC_Div_i32,
    OPC_Div_f64,
    OPC_Div_u8,

    /**
     * and(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left and r_right
     */
    OPC_And,

    /**
     * or(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left or r_right
     */
    OPC_Or,

    /**
     * branch_if_false(REG r_source, INT new_pc):
     *      if r_source != 0:
     *          pc <- new_pc
     * new_pc is relative to base_pc
     */
    OPC_Br_False,

    /**
     * branch(INT new_pc):
     *      pc <- new_pc
     * new_pc is relative to base_pc
     */
    OPC_Br,

    /**
     * call(REG r_target, INT const_addr):
     *      (look at FunctionMeta at const_addr)
     *      push stack_frame(#r_target, base_pc, pc, (*const_addr).arg_count, (*const_addr).local_count)
     *      push base_registers
     *      base_pc <- (*const_addr).base_pc
     *      pc <- (*const_addr).pc
     */
    OPC_Call,

    /**
     * return():
     *      pop stack_frame sf
     *      base_pc <- sf.base_pc
     *      pc <- sf.pc + 1
     *      save r0 to ret_val
     *      pop base_registers
     *      sf.r_target <- ret_val
     */
    OPC_Ret,

    /**
     * convert(REG r_target, REG r_source, TYPE target_type):
     *      r_target <- r_source converted to target_type (see ::Type)
     */
    OPC_Conv_i32,
    OPC_Conv_f64,
    OCP_Conv_u8,
    OPC_Conv_str,
    OPC_Conv_ref,

    /**
     * move(REG r_target, REG r_source):
     *      r_target <- r_source
     */
    OPC_Mov,

    /**
     * new_object(REG r_target, INT const_addr):
     *      (look at type_meta at const_addr)
     *      allocate memory on heap for new instance of type_meta
     *      r_target <- heap_addr of new memory block on heap (TYPE_REF)
     * const_addr is relative to const base
     */
    OPC_NewObj,

    /**
     * new_string(REG r_target, INT size):
     *      allocate memory on heap for size characters
     *      r_target <- addr of new memory block on heap (TYPE_REF)
     */
    OPC_NewStr,

    /**
     * new_array(REG r_target, INT size):
     *      allocate memory on heap for size items
     *      r_target <- heap_addr of new memory block on heap (TYPE_REF)
     */
    OPC_NewArr_i32,
    OPC_NewArr_f64,
    OPC_NewArr_u8,
    OPC_NewArr_ref,

    /**
     * add_reference(REG r_heap_addr):
     *      increment reference count for object at r_heap_addr
     */
    OPC_AddRef,

    /**
     * add_reference(REG r_heap_addr):
     *      decrement reference count for object at r_heap_addr
     *      compact heap if free space is below threshold
     */
    OPC_RemoveRef,
};

typedef byte_t OpCode;

#endif //ZLN_OPCODE_H
