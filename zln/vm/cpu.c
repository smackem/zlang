//
// Created by smackem on 22.03.21.
//

#include <memory.h>
#include <stdio.h>
#include "runtime.h"

void conv_i32(Register *target, Type target_type, const Register *source);
void conv_f64(Register *target, Type target_type, const Register *source);
void conv_u8(Register *target, Type target_type, const Register *source);
void conv_ref(Register *target, Type target_type, const Register *source);
void conv_ptr(Register *target, Type target_type, const Register *source);

addr_t exec_call(Cpu *cpu, byte_t r_ret, byte_t r_first_arg, const FunctionMeta *func, addr_t pc);
addr_t exec_virtual_call(Cpu *cpu, byte_t r_ret, byte_t r_first_arg, addr_t virtual_func_address, addr_t pc);
addr_t exec_return(Cpu *cpu);

static void init_cpu(Cpu *cpu, const MemoryLayout *memory, const RuntimeConfig *config, const FunctionMeta *entry_point) {
    byte_t *const_segment = memory->base;
    byte_t *global_segment = const_segment + memory->const_segment_size;
    byte_t *register_segment = global_segment + memory->global_segment_size;
    byte_t *stack_frame_segment = register_segment + memory->register_segment_size;
    byte_t *heap_segment = stack_frame_segment + memory->stack_frame_segment_size;
    byte_t *memory_end = memory->base + memory->total_size;
    assert(memory->stack_frame_segment_size >= config->max_stack_depth * sizeof(StackFrame));
    assert(memory->register_segment_size >= config->register_count * config->max_stack_depth * sizeof(Register));
    assert(heap_segment < memory_end);
    cpu->const_segment = const_segment;
    cpu->global_segment = global_segment;
    init_call_stack(&cpu->call_stack,
                    (Register *) register_segment,
                    (StackFrame *) stack_frame_segment,
                    config->max_stack_depth,
                    config->register_count,
                    entry_point);
    init_heap(&cpu->heap,
              heap_segment,
              memory_end - heap_segment,
              cpu->const_segment);
    trace("const_segment @ %08x\n"
          "global_segment @ %08x\n"
          "register_segment @ %08x\n"
          "stack_frame_segment @ %08x\n"
          "heap_segment @ %08x\n"
          "heap_size: %08x\n",
          (addr_t) (const_segment - memory->base),
          (addr_t) (global_segment - memory->base),
          (addr_t) (register_segment - memory->base),
          (addr_t) (stack_frame_segment - memory->base),
          (addr_t) (heap_segment - memory->base),
          (addr_t) cpu->heap.size);
}

static void free_cpu(Cpu *cpu) {
    zero_memory(cpu, sizeof(Cpu));
}

static void base_assertions() {
    assert_equal(sizeof(Instruction), INSTRUCTION_MIN_SIZE, "instruction data_size mismatch");
    assert_equal(sizeof(FunctionMeta), FUNCTION_META_MIN_SIZE, "function_meta data_size mismatch");
    assert_equal(sizeof(TypeMeta), TYPE_META_MIN_SIZE, "type_meta data_size mismatch");
    assert_equal(sizeof(HeapEntry), HEAP_ENTRY_MIN_SIZE, "heap_entry data_size mismatch");
    assert_equal(sizeof(VTableEntry), VTABLE_ENTRY_SIZE, "vtable_entry data_size mismatch");
}

static inline Register *reg(Cpu *cpu, int r) {
    assert(r >= 0 && r < cpu->call_stack.register_count);
    return &cpu->call_stack.top->registers[r];
}

const byte_t *const_segment_addr(const MemoryLayout *memory) {
    return memory->base;
}

const byte_t *global_segment_addr(const MemoryLayout *memory) {
    const byte_t *const_segment = const_segment_addr(memory);
    return const_segment + memory->const_segment_size;
}

const byte_t *heap_segment_addr(const MemoryLayout *memory) {
    const byte_t *global_segment = global_segment_addr(memory);
    const byte_t *register_segment = global_segment + memory->global_segment_size;
    const byte_t *stack_frame_segment = register_segment + memory->register_segment_size;
    return stack_frame_segment + memory->stack_frame_segment_size;
}

void execute(const byte_t *code,
             const FunctionMeta *entry_point,
             const MemoryLayout *memory,
             const RuntimeConfig *config) {
    assert(code != NULL);
    assert(entry_point != NULL);
    assert(memory != NULL);
    assert(config != NULL);
    addr_t pc = entry_point->pc;
    byte_t r_target, r_left, r_right, r_addr;
    addr_t addr;
    int32_t value;
    Type type;
    Cpu cpu;
    base_assertions();
    init_cpu(&cpu, memory, config, entry_point);

    for (;;) {
        const Instruction *instr = (const Instruction *) &code[pc];
        int size = 0;

        if (config->debug_callback != NULL) {
            uint32_t stack_depth = current_stack_depth(&cpu.call_stack);
            config->debug_callback(pc, instr, stack_depth, cpu.call_stack.top, reg(&cpu, 0), config->register_count);
        }

        switch (instr->opc) {
            // -------------------- nop
            //
            case OPC_Nop:
                size = 1 + 0;
                break;

            // -------------------- load const
            //
            case OPC_Ldc_zero:
                r_target = get_byte(instr->args, 0);
                reg(&cpu, r_target)->ptr = 0;
                size = 1 + 1;
                break;
            case OPC_Ldc_i32:
                r_target = get_byte(instr->args, 0);
                value = get_int(instr->args, 1);
                reg(&cpu, r_target)->i32 = value;
                size = 1 + 5;
                break;
            case OPC_Ldc_str: // alloc new array
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->ref = alloc_str(&cpu.heap, addr);
                size = 1 + 5;
                break;
            case OPC_Ldc_f64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->f64 = get_float(cpu.const_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load global
            //
            case OPC_LdGlb_i32:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->i32 = get_int(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LdGlb_f64:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->f64 = get_float(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LdGlb_u8:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->i32 = get_byte(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LdGlb_ref:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->ref = get_addr(cpu.global_segment, addr);
                size = 1 + 5;
                break;
            case OPC_LdGlb_ptr:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->ptr = get_ptr(cpu.global_segment, addr);
                size = 1 + 5;
                break;

            // -------------------- load field
            //
            case OPC_LdFld_i32:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                reg(&cpu, r_target)->i32 = get_int(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr));
                size = 1 + 6;
                break;
            case OPC_LdFld_f64:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                reg(&cpu, r_target)->f64 = get_float(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr));
                size = 1 + 6;
                break;
            case OPC_LdFld_u8:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                reg(&cpu, r_target)->i32 = get_byte(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr));
                size = 1 + 6;
                break;
            case OPC_LdFld_ref:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                reg(&cpu, r_target)->ref = get_addr(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr));
                size = 1 + 6;
                break;
            case OPC_LdFld_ptr:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                reg(&cpu, r_target)->ptr = get_ptr(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr));
                size = 1 + 6;
                break;

            // -------------------- load array element
            //
            case OPC_LdElem_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                reg(&cpu, r_target)->ref = get_int(cpu.heap.memory,
                  get_field_addr(&cpu.heap, reg(&cpu, r_left)->ref, reg(&cpu, r_addr)->ref * sizeof(int32_t)));
                size = 1 + 3;
                break;
            case OPC_LdElem_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                reg(&cpu, r_target)->f64 = get_float(cpu.heap.memory,
                      get_field_addr(&cpu.heap, reg(&cpu, r_left)->ref, reg(&cpu, r_addr)->ref * sizeof(double)));
                size = 1 + 3;
                break;
            case OPC_LdElem_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = get_byte(cpu.heap.memory,
                      get_field_addr(&cpu.heap, reg(&cpu, r_left)->ref, reg(&cpu, r_addr)->ref * sizeof(byte_t)));
                size = 1 + 3;
                break;
            case OPC_LdElem_ref:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                reg(&cpu, r_target)->ref = get_addr(cpu.heap.memory,
                      get_field_addr(&cpu.heap, reg(&cpu, r_left)->ref, reg(&cpu, r_addr)->ref * sizeof(addr_t)));
                size = 1 + 3;
                break;
            case OPC_LdElem_ptr:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                reg(&cpu, r_target)->ptr = get_ptr(cpu.heap.memory,
                       get_field_addr(&cpu.heap, reg(&cpu, r_left)->ref, reg(&cpu, r_addr)->ref * sizeof(intptr_t)));
                size = 1 + 3;
                break;

            // -------------------- store global
            //
            case OPC_StGlb_i32:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_int(cpu.global_segment, addr, reg(&cpu, r_left)->i32);
                size = 1 + 5;
                break;
            case OPC_StGlb_f64:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_float(cpu.global_segment, addr, reg(&cpu, r_left)->f64);
                size = 1 + 5;
                break;
            case OPC_StGlb_u8:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_byte(cpu.global_segment, addr, (byte_t) (reg(&cpu, r_left)->i32 & 0xff));
                size = 1 + 5;
                break;
            case OPC_StGlb_ref:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_addr(cpu.global_segment, addr, reg(&cpu, r_left)->ref);
                size = 1 + 5;
                break;
            case OPC_StGlb_ptr:
                r_left = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                set_ptr(cpu.global_segment, addr, reg(&cpu, r_left)->ptr);
                size = 1 + 5;
                break;

            // -------------------- store field
            //
            case OPC_StFld_i32:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_int(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr), reg(&cpu, r_left)->i32);
                size = 1 + 6;
                break;
            case OPC_StFld_f64:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_float(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr), reg(&cpu, r_left)->f64);
                size = 1 + 6;
                break;
            case OPC_StFld_u8:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_byte(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr), (byte_t) (reg(&cpu, r_left)->i32 & 0xff));
                size = 1 + 6;
                break;
            case OPC_StFld_ref:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_addr(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr), reg(&cpu, r_left)->ref);
                size = 1 + 6;
                break;
            case OPC_StFld_ptr:
                r_left = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                set_ptr(cpu.heap.memory, get_field_addr(&cpu.heap, reg(&cpu, r_addr)->ref, addr), reg(&cpu, r_left)->ptr);
                size = 1 + 6;
                break;

            // -------------------- store array element
            //
            case OPC_StElem_i32:
                r_left = get_byte(instr->args, 0);
                r_right = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                set_int(cpu.heap.memory,
                        get_field_addr(&cpu.heap, reg(&cpu, r_right)->ref, reg(&cpu, r_addr)->ref * sizeof(int32_t)),
                        reg(&cpu, r_left)->i32);
                size = 1 + 3;
                break;
            case OPC_StElem_f64:
                r_left = get_byte(instr->args, 0);
                r_right = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                set_float(cpu.heap.memory,
                        get_field_addr(&cpu.heap, reg(&cpu, r_right)->ref, reg(&cpu, r_addr)->ref * sizeof(double)),
                        reg(&cpu, r_left)->f64);
                size = 1 + 3;
                break;
            case OPC_StElem_u8:
                r_left = get_byte(instr->args, 0);
                r_right = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                set_byte(cpu.heap.memory,
                        get_field_addr(&cpu.heap, reg(&cpu, r_right)->ref, reg(&cpu, r_addr)->ref * sizeof(byte_t)),
                        reg(&cpu, r_left)->i32);
                size = 1 + 3;
                break;
            case OPC_StElem_ref:
                r_left = get_byte(instr->args, 0);
                r_right = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                set_addr(cpu.heap.memory,
                         get_field_addr(&cpu.heap, reg(&cpu, r_right)->ref, reg(&cpu, r_addr)->ref * sizeof(addr_t)),
                         reg(&cpu, r_left)->ref);
                size = 1 + 3;
                break;
            case OPC_StElem_ptr:
                r_left = get_byte(instr->args, 0);
                r_right = get_byte(instr->args, 1);
                r_addr = get_byte(instr->args, 2);
                set_ptr(cpu.heap.memory,
                        get_field_addr(&cpu.heap, reg(&cpu, r_right)->ref, reg(&cpu, r_addr)->ref * sizeof(intptr_t)),
                        reg(&cpu, r_left)->ptr);
                size = 1 + 3;
                break;

            // -------------------- add
            //
            case OPC_Add_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 + reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Add_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->f64 = reg(&cpu, r_left)->f64 + reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Add_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = (byte_t) ((reg(&cpu, r_left)->i32 + reg(&cpu, r_right)->i32) & 0xff);
                size = 1 + 3;
                break;
            case OPC_Add_str:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->ref = concat_strings(&cpu, reg(&cpu, r_left)->ref, reg(&cpu, r_right)->ref);
                size = 1 + 3;
                break;

            // -------------------- subtract
            //
            case OPC_Sub_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 - reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Sub_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->f64 = reg(&cpu, r_left)->f64 - reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Sub_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = (byte_t) ((reg(&cpu, r_left)->i32 - reg(&cpu, r_right)->i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- multiply
            //
            case OPC_Mul_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 * reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Mul_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->f64 = reg(&cpu, r_left)->f64 * reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Mul_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = (byte_t) ((reg(&cpu, r_left)->i32 * reg(&cpu, r_right)->i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- divide
            //
            case OPC_Div_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 / reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Div_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->f64 = reg(&cpu, r_left)->f64 / reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Div_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = (byte_t) ((reg(&cpu, r_left)->i32 / reg(&cpu, r_right)->i32) & 0xff);
                size = 1 + 3;
                break;

            // -------------------- equals
            //
            case OPC_Eq_i32:
            case OPC_Eq_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 == reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Eq_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->f64 == reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Eq_ref:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->ref == reg(&cpu, r_right)->ref;
                size = 1 + 3;
                break;
            case OPC_Eq_ptr:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->ptr == reg(&cpu, r_right)->ptr;
                size = 1 + 3;
                break;
            case OPC_Eq_str:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = compare_strings(&cpu, reg(&cpu, r_left)->ref, reg(&cpu, r_right)->ref) == 0;
                size = 1 + 3;
                break;

            // -------------------- not equals
            //
            case OPC_Ne_i32:
            case OPC_Ne_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 != reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Ne_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->f64 != reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Ne_ref:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->ref != reg(&cpu, r_right)->ref;
                size = 1 + 3;
                break;
            case OPC_Ne_ptr:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->ptr != reg(&cpu, r_right)->ptr;
                size = 1 + 3;
                break;
            case OPC_Ne_str:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = compare_strings(&cpu, reg(&cpu, r_left)->ref, reg(&cpu, r_right)->ref) != 0;
                size = 1 + 3;
                break;

            // -------------------- greater than
            //
            case OPC_Gt_i32:
            case OPC_Gt_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 > reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Gt_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->f64 > reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Gt_str:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = compare_strings(&cpu, reg(&cpu, r_left)->ref, reg(&cpu, r_right)->ref) > 0;
                size = 1 + 3;
                break;

            // -------------------- greater than or equal
            //
            case OPC_Ge_i32:
            case OPC_Ge_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 >= reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Ge_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->f64 >= reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Ge_str:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = compare_strings(&cpu, reg(&cpu, r_left)->ref, reg(&cpu, r_right)->ref) >= 0;
                size = 1 + 3;
                break;

            // -------------------- less than
            //
            case OPC_Lt_i32:
            case OPC_Lt_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 < reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Lt_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->f64 < reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Lt_str:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = compare_strings(&cpu, reg(&cpu, r_left)->ref, reg(&cpu, r_right)->ref) < 0;
                size = 1 + 3;
                break;

            // -------------------- less than or equal
            //
            case OPC_Le_i32:
            case OPC_Le_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 <= reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Le_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->f64 <= reg(&cpu, r_right)->f64;
                size = 1 + 3;
                break;
            case OPC_Le_str:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = compare_strings(&cpu, reg(&cpu, r_left)->ref, reg(&cpu, r_right)->ref) <= 0;
                size = 1 + 3;
                break;

            // -------------------- boolean operators
            //
            case OPC_And:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 && reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;
            case OPC_Or:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                r_right = get_byte(instr->args, 2);
                reg(&cpu, r_target)->i32 = reg(&cpu, r_left)->i32 || reg(&cpu, r_right)->i32;
                size = 1 + 3;
                break;

            // -------------------- move
            //
            case OPC_Mov:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                *reg(&cpu, r_target) = *reg(&cpu, r_left);
                size = 1 + 2;
                break;

            // -------------------- branching
            //
            case OPC_Br_zero:
                r_target = get_byte(instr->args, 0);
                if (reg(&cpu, r_target)->i32 == false) {
                    pc = get_addr(instr->args, 1);
                } else {
                    size = 1 + 5;
                }
                break;
            case OPC_Br:
                pc = get_addr(instr->args, 0);
                break;

            // -------------------- function call
            //
            case OPC_Call:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                pc = exec_call(&cpu, r_target, r_left, (FunctionMeta *) &cpu.const_segment[addr], pc);
                break;
            case OPC_CallVirt:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                addr = get_addr(instr->args, 2);
                pc = exec_virtual_call(&cpu, r_target, r_left, (VirtualFunctionMeta *) &cpu.const_segment[addr], pc);
                break;
            case OPC_Ret:
                pc = exec_return(&cpu);
                break;
            case OPC_Invoke:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                value = get_addr(instr->args, 2);
                invoke(value, &cpu, reg(&cpu, r_target), reg(&cpu, r_left));
                size = 1 + 6;
                break;
            case OPC_Halt:
                free_cpu(&cpu);
                return;

            // -------------------- convert
            //
            case OPC_Conv_i32:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_i32(reg(&cpu, r_target), type, reg(&cpu, r_left));
                size = 1 + 6;
                break;
            case OPC_Conv_f64:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_f64(reg(&cpu, r_target), type, reg(&cpu, r_left));
                size = 1 + 6;
                break;
            case OPC_Conv_u8:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_u8(reg(&cpu, r_target), type, reg(&cpu, r_left));
                size = 1 + 6;
                break;
            case OPC_Conv_ref:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_ref(reg(&cpu, r_target), type, reg(&cpu, r_left));
                size = 1 + 6;
                break;
            case OPC_Conv_ptr:
                r_target = get_byte(instr->args, 0);
                r_left = get_byte(instr->args, 1);
                type = get_int(instr->args, 2);
                conv_ptr(reg(&cpu, r_target), type, reg(&cpu, r_left));
                size = 1 + 6;
                break;

            // -------------------- alloc object
            //
            case OPC_NewObj:
                r_target = get_byte(instr->args, 0);
                addr = get_addr(instr->args, 1);
                reg(&cpu, r_target)->ref = alloc_obj(&cpu.heap, addr);
                size = 1 + 5;
                break;

            // -------------------- alloc array
            //
            case OPC_NewArr_i32:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                reg(&cpu, r_target)->ref = alloc_array(&cpu.heap, TYPE_Int32, reg(&cpu, r_addr)->i32);
                size = 1 + 2;
                break;
            case OPC_NewArr_f64:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                reg(&cpu, r_target)->ref = alloc_array(&cpu.heap, TYPE_Float64, reg(&cpu, r_addr)->i32);
                size = 1 + 2;
                break;
            case OPC_NewArr_u8:
            case OPC_NewStr:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                reg(&cpu, r_target)->ref = alloc_array(&cpu.heap, TYPE_Unsigned8, reg(&cpu, r_addr)->i32);
                size = 1 + 2;
                break;
            case OPC_NewArr_ref:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                reg(&cpu, r_target)->ref = alloc_array(&cpu.heap, TYPE_Ref, reg(&cpu, r_addr)->i32);
                size = 1 + 2;
                break;
            case OPC_NewArr_ptr:
                r_target = get_byte(instr->args, 0);
                r_addr = get_byte(instr->args, 1);
                reg(&cpu, r_target)->ref = alloc_array(&cpu.heap, TYPE_NativePtr, reg(&cpu, r_addr)->i32);
                size = 1 + 2;
                break;

            // -------------------- increment/decrement refcount
            //
            case OPC_AddRef:
                addr = get_addr(instr->args, 0);
                add_ref(&cpu.heap, addr);
                size = 1 + 4;
                break;
            case OPC_RemoveRef:
                addr = get_addr(instr->args, 0);
                remove_ref(&cpu.heap, addr);
                size = 1 + 4;
                break;

            default:
                assert_that(false, "pc=%08x: unsupported opcode %d", pc, instr->opc);
                break;
        }

        pc += size;
    }
}

inline addr_t exec_call(Cpu *cpu, byte_t r_ret_val, byte_t r_first_arg, const FunctionMeta *func, addr_t pc) {
    const StackFrame *old_top = cpu->call_stack.top;
    assert(old_top != NULL);
    StackFrame *top = push_stack_frame(&cpu->call_stack,
                                       func,
                                       r_ret_val,
                                       pc + 1 + 6); // add data_size of call instr
    memcpy(&top->registers[1], &old_top->registers[r_first_arg], func->arg_count * sizeof(Register));
    return func->pc;
}

inline addr_t exec_virtual_call(Cpu *cpu, byte_t r_ret_val, byte_t r_first_arg, addr_t virtual_func_addr, addr_t pc) {
    const StackFrame *old_top = cpu->call_stack.top;
    assert(old_top != NULL);
    addr_t self = reg(cpu, r_first_arg)->ref;
    const FunctionMeta *func = get_impl_function(&cpu->heap, self, virtual_func_addr);
    assert(func != NULL);
    return exec_call(cpu, r_ret_val, r_first_arg, func, pc);
}

inline addr_t exec_return(Cpu *cpu) {
    const StackFrame *old_top = pop_stack_frame(&cpu->call_stack);
    assert(old_top != NULL);
    StackFrame *top = cpu->call_stack.top;
    // store return value
    if (old_top->meta->ret_type != TYPE_Void) {
        top->registers[old_top->r_ret_val] = old_top->registers[0];
    }
    return old_top->ret_pc;
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
            target->i32 = source->i32;
            break;
        case TYPE_Unsigned8:
            target->i32 = source->i32 & 0xff;
            break;
        default:
            assert_that(false, "conv_i32: unsupported target type %d", target_type);
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
            assert_that(false, "conv_f64: unsupported target type %d", target_type);
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
            assert_that(false, "conv_u8: unsupported target type %d", target_type);
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
            assert_that(false, "conv_ref: unsupported target type %d", target_type);
            break;
    }
}

inline void conv_ptr(Register *target, Type target_type, const Register *source) {
    assert_that(target_type == TYPE_Ref, "conv_ref: unsupported target type %d", target_type);
}
