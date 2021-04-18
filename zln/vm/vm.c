//
// Created by smackem on 24.03.21.
//

#include <stdio.h>
#include "vm.h"

static const char *const opcode_names[] = {
    "Nop",
    "LdGlb_i32",
    "LdGlb_f64",
    "LdGlb_u8",
    "LdGlb_ref",
    "LdGlb_ptr",
    "LdFld_i32",
    "LdFld_f64",
    "LdFld_u8",
    "LdFld_ref",
    "LdFld_ptr",
    "LdElem_i32",
    "LdElem_f64",
    "LdElem_u8",
    "LdElem_ref",
    "LdElem_ptr",
    "StGlb_i32",
    "StGlb_f64",
    "StGlb_u8",
    "StGlb_ref",
    "StGlb_ptr",
    "StFld_i32",
    "StFld_f64",
    "StFld_u8",
    "StFld_ref",
    "StFld_ptr",
    "StElem_i32",
    "StElem_f64",
    "StElem_u8",
    "StElem_ref",
    "StElem_ptr",
    "Ldc_i32",
    "Ldc_ref",
    "Ldc_f64",
    "Ldc_zero",
    "Add_i32",
    "Add_f64",
    "Add_u8",
    "Add_str",
    "Sub_i32",
    "Sub_f64",
    "Sub_u8",
    "Mul_i32",
    "Mul_f64",
    "Mul_u8",
    "Div_i32",
    "Div_f64",
    "Div_u8",
    "Eq_i32",
    "Eq_f64",
    "Eq_u8",
    "Eq_str",
    "Eq_ref",
    "Eq_ptr",
    "Ne_i32",
    "Ne_f64",
    "Ne_u8",
    "Ne_str",
    "Ne_ref",
    "Ne_ptr",
    "Gt_i32",
    "Gt_f64",
    "Gt_u8",
    "Gt_str",
    "Ge_i32",
    "Ge_f64",
    "Ge_u8",
    "Ge_str",
    "Lt_i32",
    "Lt_f64",
    "Lt_u8",
    "Lt_str",
    "Le_i32",
    "Le_f64",
    "Le_u8",
    "Le_str",
    "And",
    "Or",
    "Mov",
    "Br_zero",
    "Br",
    "Call",
    "Ret",
    "Halt",
    "Conv_i32",
    "Conv_f64",
    "Conv_u8",
    "Conv_str",
    "Conv_ref",
    "Conv_ptr",
    "NewObj",
    "NewStr",
    "NewArr_i32",
    "NewArr_f64",
    "NewArr_u8",
    "NewArr_ref",
    "NewArr_ptr",
    "AddRef",
    "RemoveRef",
};

static const char *const type_names[] = {
    "NIL",
    "I32",
    "F64",
    "UI8",
    "STR",
    "REF",
    "PTR",
};

const char *opcode_name(OpCode opc) {
    return opcode_names[opc];
}

const char *type_name(Type type) {
    return type_names[type];
}

size_t print_instruction(FILE *f, const Instruction *instr) {
    size_t size = 0;
    switch (instr->opc) {
        case OPC_Nop:
            fprintf(f, "%12s", opcode_name(instr->opc));
            return 1 + 0;
        case OPC_Ldc_zero:
            fprintf(f, "%12s r%d",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0));
            return 1 + 1;
        case OPC_Ldc_i32:
        case OPC_Ldc_ref:
        case OPC_Ldc_f64:
        case OPC_LdGlb_i32:
        case OPC_LdGlb_f64:
        case OPC_LdGlb_u8:
        case OPC_LdGlb_ref:
        case OPC_LdGlb_ptr:
        case OPC_StGlb_i32:
        case OPC_StGlb_f64:
        case OPC_StGlb_u8:
        case OPC_StGlb_ref:
        case OPC_StGlb_ptr:
            fprintf(f, "%12s r%d %08x",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_int(instr->args, 1));
            return 1 + 5;
        case OPC_LdFld_i32:
        case OPC_LdFld_f64:
        case OPC_LdFld_u8:
        case OPC_LdFld_ref:
        case OPC_LdFld_ptr:
        case OPC_StFld_i32:
        case OPC_StFld_f64:
        case OPC_StFld_u8:
        case OPC_StFld_ref:
        case OPC_StFld_ptr:
            fprintf(f, "%12s r%d r%d %08x",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_byte(instr->args, 1),
                    get_int(instr->args, 2));
            return 1 + 6;
        case OPC_LdElem_i32:
        case OPC_LdElem_f64:
        case OPC_LdElem_u8:
        case OPC_LdElem_ref:
        case OPC_LdElem_ptr:
        case OPC_StElem_i32:
        case OPC_StElem_f64:
        case OPC_StElem_u8:
        case OPC_StElem_ref:
        case OPC_StElem_ptr:
        case OPC_Add_i32:
        case OPC_Add_f64:
        case OPC_Add_u8:
        case OPC_Sub_i32:
        case OPC_Sub_f64:
        case OPC_Sub_u8:
        case OPC_Mul_i32:
        case OPC_Mul_f64:
        case OPC_Mul_u8:
        case OPC_Div_i32:
        case OPC_Div_f64:
        case OPC_Div_u8:
        case OPC_Eq_i32:
        case OPC_Eq_u8:
        case OPC_Eq_f64:
        case OPC_Eq_ref:
        case OPC_Eq_ptr:
        case OPC_Ne_i32:
        case OPC_Ne_u8:
        case OPC_Ne_f64:
        case OPC_Ne_ref:
        case OPC_Ne_ptr:
        case OPC_Gt_i32:
        case OPC_Gt_u8:
        case OPC_Gt_f64:
        case OPC_Ge_i32:
        case OPC_Ge_u8:
        case OPC_Ge_f64:
        case OPC_Lt_i32:
        case OPC_Lt_u8:
        case OPC_Lt_f64:
        case OPC_Le_i32:
        case OPC_Le_u8:
        case OPC_Le_f64:
        case OPC_And:
        case OPC_Or:
            fprintf(f, "%12s r%d r%d r%d",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_byte(instr->args, 1),
                    get_byte(instr->args, 2));
            return 1 + 3;
        case OPC_Mov:
            fprintf(f, "%12s r%d r%d",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_byte(instr->args, 1));
            return 1 + 2;
        case OPC_Br_zero:
            fprintf(f, "%12s r%d %08x",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_int(instr->args, 1));
            return 1 + 5;
        case OPC_Br:
            fprintf(f, "%12s %08x",
                    opcode_name(instr->opc),
                    get_int(instr->args, 0));
            return 1 + 4;
        case OPC_Call:
            fprintf(f, "%12s r%d r%d %08x",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_byte(instr->args, 1),
                    get_addr(instr->args, 2));
            return 1 + 6;
        case OPC_Ret:
        case OPC_Halt:
            fprintf(f, "%12s", opcode_name(instr->opc));
            return 1 + 0;
        case OPC_Conv_i32:
        case OPC_Conv_f64:
        case OPC_Conv_u8:
        case OPC_Conv_str:
        case OPC_Conv_ref:
        case OPC_Conv_ptr:
            fprintf(f, "%12s r%d r%d %s",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_byte(instr->args, 1),
                    type_name(get_int(instr->args, 2)));
            return 1 + 6;
        case OPC_NewObj:
            fprintf(f, "%12s r%d %08x",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_int(instr->args, 1));
            return 1 + 5;
        case OPC_NewArr_i32:
        case OPC_NewArr_f64:
        case OPC_NewArr_u8:
        case OPC_NewArr_ref:
        case OPC_NewArr_ptr:
            fprintf(f, "%12s r%d r%d",
                    opcode_name(instr->opc),
                    get_byte(instr->args, 0),
                    get_byte(instr->args, 1));
            return 1 + 2;
        default:
            assert_that(false, "unsupported opcode %d", instr->opc);
            break;
    }
    return size;
}

void print_code(FILE *f, const byte_t *code, size_t code_size) {
    for (size_t offset = 0; offset < code_size; ) {
        const Instruction *instr = (const Instruction *) (code + offset);
        fprintf(f, "%08lx ", offset);
        offset += print_instruction(f, instr);
        fputc('\n', f);
    }
}

void print_registers(FILE *f, const Register *registers, int count) {
    for (int i = 0; i < count; i++, registers++) {
        fprintf(f, "r%02d = %08x|%lf\n", i, registers->i32, registers->f64);
    }
}

void dump_cpu(addr_t pc,
              addr_t base_pc,
              const Instruction *instr,
              size_t stack_depth,
              const StackFrame *stack_frame,
              const Register *registers,
              size_t register_count) {
    fprintf(stdout, "-----------------------");
    for (stack_frame -= stack_depth - 1; stack_depth > 0; stack_depth--, stack_frame++) {
        fprintf(stdout, " %s", stack_frame->meta->name);
    }
    fputc('\n', stdout);
    print_registers(stdout, registers, (int) register_count);
    fprintf(stdout, "%08x ", base_pc + pc);
    print_instruction(stdout, instr);
    fputc('\n', stdout);
}
