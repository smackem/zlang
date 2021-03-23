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
    byte_t *dynamic_segment;
    addr_t dynamic_size;
} Cpu;

static void init_cpu(Cpu *cpu, const HeapLayout *heap, const RuntimeConfig *config) {
    cpu->base_registers = calloc(config->register_count * config->max_stack_depth, sizeof(Register));
    cpu->registers = cpu->base_registers;
    cpu->stack_frames = calloc(config->max_stack_depth, sizeof(StackFrame));
    cpu->stack_frame = cpu->stack_frames;
    cpu->const_segment = heap->memory;
    cpu->global_segment = heap->memory + heap->const_segment_size;
    cpu->dynamic_segment = cpu->global_segment + heap->global_segment_size;
    cpu->dynamic_size = 0;
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
    assert(sizeof(Instruction) == INSTRUCTION_MIN_SIZE,
           "instruction size %lu != %lu", sizeof(Instruction), INSTRUCTION_MIN_SIZE,
           INSTRUCTION_MIN_SIZE);
    assert(sizeof(FunctionMeta) == FUNCTION_META_SIZE,
           "function_meta size != %lu",
           FUNCTION_META_SIZE);
}

void execute(const byte_t *code,
             addr_t base_pc,
             addr_t pc,
             const HeapLayout *heap,
             const RuntimeConfig *config) {
    byte_t r_target, r_left, r_right, r_addr;
    addr_t addr;
    int32_t value;
    Type type;
    Cpu cpu;
    base_assertions();
    init_cpu(&cpu, heap, config);

    for (;;) {
        const Instruction *instr = (const Instruction *) &code[base_pc + pc];
        int size = 0;
        switch (instr->opc) {
            // -------------------- nop
            //
            case OPC_NOP:
                size = 1 + 0;
                break;

            // -------------------- load const
            //
            case OPC_LD_C_I32:
                r_target = get_byte(instr->args, 0);
                value = get_int(instr->args, 1);
                cpu.registers[r_target].i32 = value;
                size = 1 + 5;
                break;
            case OPC_LD_C_REF:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].ref = addr;
                size = 1 + 5;
                break;
            case OPC_LD_C_F64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].f64 = get_float(cpu.const_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load global
            //
            case OPC_LD_GLB_I32:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].i32 = get_int(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LD_GLB_F64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].f64 = get_float(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LD_GLB_U8:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].i32 = get_byte(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LD_GLB_REF:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                cpu.registers[r_target].ref = get_addr(cpu.global_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load field
            //
            case OPC_LD_FLD_I32:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].i32 = get_int(heap->memory, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LD_FLD_F64:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].f64 = get_float(heap->memory, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LD_FLD_U8:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].i32 = get_byte(heap->memory, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LD_FLD_REF:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                cpu.registers[r_target].ref = get_addr(heap->memory, cpu.registers[r_addr].ref + addr);
                size = 1 + 6;
                break;

            // -------------------- store global
            //
            case OPC_ST_GLB_I32:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_int(cpu.global_segment, addr, cpu.registers[r_left].i32);
                size = 1 + 5;
                break;
            case OPC_ST_GLB_F64:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_float(cpu.global_segment, addr, cpu.registers[r_left].f64);
                size = 1 + 5;
                break;
            case OPC_ST_GLB_U8:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_byte(cpu.global_segment, addr, (byte_t) (cpu.registers[r_left].i32 & 0xff));
                size = 1 + 5;
                break;
            case OPC_ST_GLB_REF:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_addr(cpu.global_segment, addr, cpu.registers[r_left].ref);
                size = 1 + 5;
                break;

            // -------------------- store field
            //
            case OPC_ST_FLD_I32:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_int(heap->memory, cpu.registers[r_addr].ref + addr, cpu.registers[r_left].i32);
                size = 1 + 6;
                break;
            case OPC_ST_FLD_F64:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_float(heap->memory, cpu.registers[r_addr].ref + addr, cpu.registers[r_left].f64);
                size = 1 + 6;
                break;
            case OPC_ST_FLD_U8:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_byte(heap->memory, cpu.registers[r_addr].ref + addr, (byte_t) (cpu.registers[r_left].i32 & 0xff));
                size = 1 + 6;
                break;
            case OPC_ST_FLD_REF:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_addr(heap->memory, cpu.registers[r_addr].ref + addr, cpu.registers[r_left].ref);
                size = 1 + 6;
                break;

            // -------------------- add
            //
            case OPC_ADD_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 + cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_ADD_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 + cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_ADD_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 + cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- subtract
            //
            case OPC_SUB_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 - cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_SUB_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 - cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_SUB_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 - cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- multiply
            //
            case OPC_MUL_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 * cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_MUL_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 * cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_MUL_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 * cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- divide
            //
            case OPC_DIV_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = cpu.registers[r_left].i32 / cpu.registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_DIV_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].f64 = cpu.registers[r_left].f64 / cpu.registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_DIV_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                cpu.registers[r_target].i32 = (byte_t) ((cpu.registers[r_left].i32 / cpu.registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- function call
            //
            case OPC_RET:
                free_cpu(&cpu);
                return;

            default:
                assert(false, "unsupported opcode %d", instr->opc);
                break;
        }

        assert(size > 0, "op code %d has not been handled", instr->opc);
        pc += size;
    }
}
