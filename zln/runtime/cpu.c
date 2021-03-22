//
// Created by smackem on 22.03.21.
//

#include "cpu.h"

static Register *base_registers = NULL;
static Register *registers;
static StackFrame *stack_frames = NULL;
static StackFrame *stack_frame;
static const byte_t *const_segment;
static byte_t *global_segment;
static byte_t *dynamic_segment;
static addr_t dynamic_size;

void base_assertions() {
    assert(sizeof(Instruction) == INSTRUCTION_MIN_SIZE,
           "instruction size != %lu",
           INSTRUCTION_MIN_SIZE);
    assert(sizeof(FunctionMeta) == FUNCTION_META_SIZE,
           "function_meta size != %lu",
           FUNCTION_META_SIZE);
}

static void init(const HeapLayout *heap, const RuntimeConfig *config) {
    base_assertions();
    base_registers = calloc(config->register_count * config->max_stack_depth, sizeof(Register));
    registers = base_registers;
    stack_frames = calloc(config->max_stack_depth, sizeof(StackFrame));
    stack_frame = stack_frames;
    const_segment = heap->memory;
    global_segment = heap->memory + heap->const_segment_size;
    dynamic_segment = global_segment + heap->global_segment_size;
    dynamic_size = 0;
}

static void close() {
    if (base_registers != NULL) {
        free(base_registers);
        base_registers = NULL;
    }
    if (stack_frames != NULL) {
        free(stack_frames);
        stack_frames = NULL;
    }
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
    init(heap, config);
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
                registers[r_target].i32 = value;
                size = 1 + 5;
                break;
            case OPC_LD_C_REF:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                registers[r_target].ref = addr;
                size = 1 + 5;
                break;
            case OPC_LD_C_F64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                registers[r_target].f64 = get_float(const_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load global
            //
            case OPC_LD_GLB_I32:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                registers[r_target].i32 = get_int(global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LD_GLB_F64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                registers[r_target].f64 = get_float(global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LD_GLB_U8:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                registers[r_target].i32 = get_byte(global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LD_GLB_REF:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                registers[r_target].ref = get_addr(global_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load field
            //
            case OPC_LD_FLD_I32:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                registers[r_target].i32 = get_int(heap->memory, registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LD_FLD_F64:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                registers[r_target].f64 = get_float(heap->memory, registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LD_FLD_U8:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                registers[r_target].i32 = get_byte(heap->memory, registers[r_addr].ref + addr);
                size = 1 + 6;
                break;
            case OPC_LD_FLD_REF:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                registers[r_target].ref = get_addr(heap->memory, registers[r_addr].ref + addr);
                size = 1 + 6;
                break;

            // -------------------- store global
            //
            case OPC_ST_GLB_I32:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_int(global_segment, addr, registers[r_left].i32);
                size = 1 + 5;
                break;
            case OPC_ST_GLB_F64:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_float(global_segment, addr, registers[r_left].f64);
                size = 1 + 5;
                break;
            case OPC_ST_GLB_U8:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_byte(global_segment, addr, (byte_t) (registers[r_left].i32 & 0xff));
                size = 1 + 5;
                break;
            case OPC_ST_GLB_REF:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_addr(global_segment, addr, registers[r_left].ref);
                size = 1 + 5;
                break;

            // -------------------- store field
            //
            case OPC_ST_FLD_I32:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_int(heap->memory, registers[r_addr].ref + addr, registers[r_left].i32);
                size = 1 + 6;
                break;
            case OPC_ST_FLD_F64:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_float(heap->memory, registers[r_addr].ref + addr, registers[r_left].f64);
                size = 1 + 6;
                break;
            case OPC_ST_FLD_U8:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_byte(heap->memory, registers[r_addr].ref + addr, (byte_t) (registers[r_left].i32 & 0xff));
                size = 1 + 6;
                break;
            case OPC_ST_FLD_REF:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_addr(heap->memory, registers[r_addr].ref + addr, registers[r_left].ref);
                size = 1 + 6;
                break;

            // -------------------- add
            //
            case OPC_ADD_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = registers[r_left].i32 + registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_ADD_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].f64 = registers[r_left].f64 + registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_ADD_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = (byte_t) ((registers[r_left].i32 + registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- subtract
            //
            case OPC_SUB_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = registers[r_left].i32 - registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_SUB_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].f64 = registers[r_left].f64 - registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_SUB_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = (byte_t) ((registers[r_left].i32 - registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- multiply
            //
            case OPC_MUL_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = registers[r_left].i32 * registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_MUL_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].f64 = registers[r_left].f64 * registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_MUL_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = (byte_t) ((registers[r_left].i32 * registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- divide
            //
            case OPC_DIV_I32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = registers[r_left].i32 / registers[r_right].i32;
                size = 1 + 3;
                break;
            case OPC_DIV_F64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].f64 = registers[r_left].f64 / registers[r_right].f64;
                size = 1 + 3;
                break;
            case OPC_DIV_U8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                registers[r_target].i32 = (byte_t) ((registers[r_left].i32 / registers[r_right].i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- function call
            //
            case OPC_RET:
                close();
                return;

            default:
                assert(false, "unsupported opcode %d", instr->opc);
                break;
        }

        assert(size > 0, "op code %d has not been handled", instr->opc);
        pc += size;
    }
}
