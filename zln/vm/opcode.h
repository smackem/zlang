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
     * glb_addr is relative to global segment base
     */
    OPC_LdGlb_i32,
    OPC_LdGlb_f64,
    OPC_LdGlb_u8,
    OPC_LdGlb_ref,
    OPC_LdGlb_ptr,

    /**
     * load_field(REG r_target, REG r_heap_addr, INT field_offset):
     *      r_target <- *(r_heap_addr + field_offset)
     * r_heap_addr is relative to heap base
     */
    OPC_LdFld_i32,
    OPC_LdFld_f64,
    OPC_LdFld_u8,
    OPC_LdFld_ref,
    OPC_LdFld_ptr,

    /**
     * load_array_element(REG r_target, REG r_heap_addr, REG r_elem_offset):
     *      r_target <- *(r_heap_addr + r_elem_offset)
     * r_heap_addr is relative to heap base. r_elem_offset is a byte offset.
     */
    OPC_LdElem_i32,
    OPC_LdElem_f64,
    OPC_LdElem_u8,
    OPC_LdElem_ref,
    OPC_LdElem_ptr,

    /**
     * store_global(REG r_source, INT glb_addr):
     *      r_source -> *glb_addr
     * glb_addr is relative to global base
     */
    OPC_StGlb_i32,
    OPC_StGlb_f64,
    OPC_StGlb_u8,
    OPC_StGlb_ref,
    OPC_StGlb_ptr,

    /**
     * store_field(REG r_source, REG r_heap_addr, INT field_offset):
     *      r_source -> *(r_heap_addr + field_offset)
     * r_heap_addr is relative to heap base
     */
    OPC_StFld_i32,
    OPC_StFld_f64,
    OPC_StFld_u8,
    OPC_StFld_ref,
    OPC_StFld_ptr,

    /**
     * store_array_element(REG r_source, REG r_heap_addr, REG r_elem_offset):
     *      r_source -> *(r_heap_addr + r_elem_offset)
     * r_heap_addr is relative to heap base. r_elem_offset is a byte offset.
     */
    OPC_StElem_i32,
    OPC_StElem_f64,
    OPC_StElem_u8,
    OPC_StElem_ref,
    OPC_StElem_ptr,

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
     * load_constant_zero(REG r_target):
     *      r_target <- 0 (zero entire register)
     *      works for all data types
     */
    OPC_Ldc_zero,

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
     * equals(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left == r_right
     */
    OPC_Eq_i32,
    OPC_Eq_f64,
    OPC_Eq_u8,
    OPC_Eq_str,
    OPC_Eq_ref,
    OPC_Eq_ptr,

    /**
     * not_equals(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left != r_right
     */
    OPC_Ne_i32,
    OPC_Ne_f64,
    OPC_Ne_u8,
    OPC_Ne_str,
    OPC_Ne_ref,
    OPC_Ne_ptr,

    /**
     * greater_than(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left > r_right
     */
    OPC_Gt_i32,
    OPC_Gt_f64,
    OPC_Gt_u8,
    OPC_Gt_str,

    /**
     * greater_than_or_equal(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left >= r_right
     */
    OPC_Ge_i32,
    OPC_Ge_f64,
    OPC_Ge_u8,
    OPC_Ge_str,

    /**
     * less_than(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left > r_right
     */
    OPC_Lt_i32,
    OPC_Lt_f64,
    OPC_Lt_u8,
    OPC_Lt_str,

    /**
     * less_than_or_equal(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left >= r_right
     */
    OPC_Le_i32,
    OPC_Le_f64,
    OPC_Le_u8,
    OPC_Le_str,

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
     * move(REG r_target, REG r_source):
     *      r_target <- r_source
     */
    OPC_Mov,

    /**
     * branch_if_false(REG r_source, INT new_pc):
     *      if r_source.i32 != 0:
     *          pc <- new_pc
     * new_pc is relative to base_pc
     */
    OPC_Br_zero,

    /**
     * branch(INT new_pc):
     *      pc <- new_pc
     * new_pc is relative to base_pc
     */
    OPC_Br,

    /**
     * call(REG r_target, REG r_first_arg, INT const_addr):
     *      (look at FunctionMeta at const_addr)
     *      push stack_frame(#r_target, base_pc, pc, FunctionMeta)
     *      copy arguments: registers r_first_arg..r_first_arg + FunctionMeta.arg_count
     *              to new stack frame
     *      base_pc <- FunctionMeta.base_pc
     *      pc <- FunctionMeta.pc
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
     * halt():
     *      stop program execution
     */
    OPC_Halt,

    /**
     * convert(REG r_target, REG r_source, TYPE target_type):
     *      r_target <- r_source converted to target_type (see ::Type)
     */
    OPC_Conv_i32,
    OPC_Conv_f64,
    OPC_Conv_u8,
    OPC_Conv_str,
    OPC_Conv_ref,
    OPC_Conv_ptr,

    /**
     * new_object(REG r_target, INT const_addr):
     *      (look at type_meta at const_addr)
     *      allocate memory on heap for new instance of type_meta
     *      r_target <- heap_addr of new memory block on heap (TYPE_REF)
     * const_addr is relative to const base
     */
    OPC_NewObj,

    /**
     * new_string(REG r_target, INT data_size):
     *      allocate memory on heap for data_size characters
     *      r_target <- addr of new memory block on heap (TYPE_REF)
     */
    OPC_NewStr,

    /**
     * new_array(REG r_target, REG r_size):
     *      allocate memory on heap for *r_size items
     *      r_target <- heap_addr of new memory block on heap (TYPE_REF)
     */
    OPC_NewArr_i32,
    OPC_NewArr_f64,
    OPC_NewArr_u8,
    OPC_NewArr_ref,
    OPC_NewArr_ptr,

    /**
     * add_reference(REG r_heap_addr):
     *      increment reference count for object at r_heap_addr
     */
    OPC_AddRef,

    /**
     * remove_reference(REG r_heap_addr):
     *      decrement reference count for object at r_heap_addr
     */
    OPC_RemoveRef,
};

typedef byte_t OpCode;

#endif //ZLN_OPCODE_H
