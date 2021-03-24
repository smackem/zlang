//
// Created by smackem on 22.03.21.
//

#include <memory.h>
#include "cpu.h"

typedef struct cpu {
    Register *base_registers;
    Register *registers;
    StackFrame *stack_frames;
    StackFrame *stack_frame;
    const byte_t *const_segment;
    byte_t *global_segment;
    byte_t *heap_segment;
    addr_t heap_size;
} Cpu;

void conv_i32(Register *target, Type target_type, const Register *source);
void conv_f64(Register *target, Type target_type, const Register *source);
void conv_u8(Register *target, Type target_type, const Register *source);
void conv_ref(Register *target, Type target_type, const Register *source);

static void init_cpu(Cpu *cpu, const MemoryLayout *memory, const RuntimeConfig *config) {
    cpu->base_registers = calloc(config->register_count * config->max_stack_depth, sizeof(Register));
    cpu->registers = cpu->base_registers;
    cpu->stack_frames = calloc(config->max_stack_depth, sizeof(StackFrame));
    cpu->stack_frame = cpu->stack_frames;
    cpu->const_segment = memory->base;
    cpu->global_segment = memory->base + memory->const_segment_size;
    cpu->heap_segment = cpu->global_segment + memory->global_segment_size;
    cpu->heap_size = 0;
}

static void free_cpu(Cpu *cpu) {
    if (cpu->base_registers != NULL) {
        free(cpu->base_registers);
    }
    if (cpu->stack_frames != NULL) {
        free(cpu->stack_frames);
    }
    bzero(cpu, sizeof(Cpu));
}

void base_assertions() {
    assert_equal(sizeof(Instruction), INSTRUCTION_MIN_SIZE, "instruction size mismatch");
    assert_equal(sizeof(FunctionMeta), FUNCTION_META_SIZE, "function_meta size mismatch");
    assert_equal(sizeof(TypeMeta), TYPE_META_MIN_SIZE, "type_meta size mismatch");
    assert_equal(sizeof(HeapEntry), HEAP_ENTRY_MIN_SIZE, "heap_entry size mismatch");
}

void execute(const byte_t *code,
             addr_t base_pc,
             addr_t pc,
             const MemoryLayout *memory,
             const RuntimeConfig *config) {
    byte_t r_target, r_left, r_right, r_addr;
    addr_t addr;
    int32_t value;
    Type type;
    Cpu cpu;
    base_assertions();
    init_cpu(&cpu, memory, config);

    for (;;) {
        const Instruction *instr = (const Instruction *) &code[base_pc + pc];
        int size = 0;
        switch (instr->opc) {
            // -------------------- nop
            //
            case OPC_Nop:
                size = 1 + 0;
                break;

            // -------------------- load const
            //
            case OPC_Ldc_i32:
                r_target = get_byte(instr->args, 0);
                value = get_int(instr->args, 1);
                cpu.registers[r_target].i32 = value;
                size = 1 + 5;
                break;
            case OPC_Ldc_ref:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].ref = addr;
                size = 1 + 5;
                break;
            case OPC_Ldc_f64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].f64 = get_float(cpu.const_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load global
            //
            case OPC_LdGlb_i32:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].i32 = get_int(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LdGlb_f64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].f64 = get_float(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LdGlb_u8:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].i32 = get_byte(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LdGlb_ref:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].ref = get_addr(cpu.global_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load field
            //
            case OPC_LdFld_i32:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].i32 = get_int(memory->base, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LdFld_f64:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].f64 = get_float(memory->base, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LdFld_u8:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].i32 = get_byte(memory->base, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LdFld_ref:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].ref = get_addr(memory->base, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;

            // -------------------- store global
            //
            case OPC_StGlb_i32:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_int(cpu.global_segment, addr, cpu.registers[r_left].i32);
                size = 1 + 5;
                break;
            case OPC_StGlb_f64:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_float(cpu.global_segment, addr, cpu.registers[r_left].f64);
                size = 1 + 5;
                break;
            case OPC_StGlb_u8:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_byte(cpu.global_segment, addr, (byte_t) (cpu.registers[r_left].i32 & 0xff));
                size = 1 + 5;
                break;
            case OPC_StGlb_ref:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_addr(cpu.global_segment, addr, cpu.registers[r_left].ref);
                size = 1 + 5;
                break;

            // -------------------- store field
            //
            case OPC_StFld_i32:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_int(memory->base, cpu.registers[r_addr].ref + addr, cpu.registers[r_left].i32);
                size = 1 + 6;
                break;
            case OPC_StFld_f64:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_float(memory->base, cpu.registers[r_addr].ref + addr, cpu.registers[r_left].f64);
                size = 1 + 6;
                break;
            case OPC_StFld_u8:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_byte(memory->base, cpu.registers[r_addr].ref + addr, (byte_t) (cpu.registers[r_left].i32 & 0xff));
                size = 1 + 6;
                break;
            case OPC_StFld_ref:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_addr(memory->base, cpu.registers[r_addr].ref + addr, cpu.registers[r_left].ref);
                size = 1 + 6;
                break;

            // -------------------- add
            //
            case OPC_Add_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 + cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_Add_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 + cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_Add_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 + cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- subtract
            //
            case OPC_Sub_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 - cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_Sub_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 - cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_Sub_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 - cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- multiply
            //
            case OPC_Mul_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 * cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_Mul_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 * cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_Mul_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 * cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- divide
            //
            case OPC_Div_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 / cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_Div_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 / cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_Div_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 / cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- boolean operators
            //
            case OPC_And:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 && cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_Or:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 || cpu.registers[r_right].i32;
                size = 1 + 3;
                break;

            // -------------------- move
            //
            case OPC_Mov:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32;
                size = 1 + 2;
                break;

            // -------------------- branching
            //
            case OPC_Br_False:
                r_target = get_byte(instr->args, 0);
                if (cpu.registers[r_target].i32 == false) {
                    pc = get_addr(instr->args, 1);
                }
                size = 1 + 5;
                break;
            case OPC_Br:
                pc = get_addr(instr->args, 0);
                size = 1 + 4;
                break;

            // -------------------- function call
            //
            case OPC_Ret:
                free_cpu(&cpu);
                return;

            // -------------------- convert
            //
            case OPC_Conv_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_i32(&cpu.registers[r_target], type, &cpu.registers[r_left]);
                size = 1 + 6;
                break;
            case OPC_Conv_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_f64(&cpu.registers[r_target], type, &cpu.registers[r_left]);
                size = 1 + 6;
                break;
            case OCP_Conv_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_u8(&cpu.registers[r_target], type, &cpu.registers[r_left]);
                size = 1 + 6;
                break;
            case OPC_Conv_ref:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_ref(&cpu.registers[r_target], type, &cpu.registers[r_left]);
                size = 1 + 6;
                break;

            default:
                assert(false, "unsupported opcode %d", instr->opc);
                break;
        }

        assert(size > 0, "op code %d has not been handled", instr->opc);
        pc += size;
    }
}

inline void conv_i32(Register *target, Type target_type, const Register *source) {
    switch (target_type) {
        case TYPE_Float64:
            target->f64 = (double) source->i32;
            break;
        case TYPE_Ref:
            target->ref = (addr_t) source->i32;
            break;
        case TYPE_Int32:
        case TYPE_Unsigned8:
            target->i32 = source->i32;
            break;
        default:
            assert(false, "conv_i32: unsupported target type %d", target_type);
            break;
    }
}

inline void conv_f64(Register *target, Type target_type, const Register *source) {
    switch (target_type) {
        case TYPE_Float64:
            target->f64 = source->f64;
            break;
        case TYPE_Ref:
            target->ref = (addr_t) source->f64;
            break;
        case TYPE_Int32:
        case TYPE_Unsigned8:
            target->i32 = (int32_t) source->f64;
            break;
        default:
            assert(false, "conv_f64: unsupported target type %d", target_type);
            break;
    }
}

inline void conv_u8(Register *target, Type target_type, const Register *source) {
    switch (target_type) {
        case TYPE_Float64:
            target->f64 = (double) (source->i32 & 0xff);
            break;
        case TYPE_Ref:
            target->ref = (addr_t) (source->i32 & 0xff);
            break;
        case TYPE_Int32:
        case TYPE_Unsigned8:
            target->i32 = source->i32 & 0xff;
            break;
        default:
            assert(false, "conv_u8: unsupported target type %d", target_type);
            break;
    }
}

inline void conv_ref(Register *target, Type target_type, const Register *source) {
    switch (target_type) {
        case TYPE_Float64:
            target->f64 = (double) source->ref;
            break;
        case TYPE_Ref:
            target->ref = source->ref;
            break;
        case TYPE_Int32:
        case TYPE_Unsigned8:
            target->i32 = (int32_t) source->ref;
            break;
        default:
            assert(false, "conv_ref: unsupported target type %d", target_type);
            break;
    }
}
