//
// Created by smackem on 23.03.21.
//
#include <stdio.h>
#include <build_config.h>
#include <util.h>
#include <memory.h>
#include "emit.h"

static RuntimeConfig config = { .register_count = 8, .max_stack_depth = 256 };

static FunctionMeta default_entry_point = {
        .pc = 0,
        .arg_count = 0,
        .local_count = 0,
        .name = "main",
};

// ---------------------------------------------------------------------
// TEST 1
// - load integer constants and store globals
// - add integers
// ---------------------------------------------------------------------

void test01(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i0 = 0;
    const addr_t glb_i1 = 4;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 100);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 200);
    code_ptr += emit_reg3(code_ptr, OPC_Add_i32, 1, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i0);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 2, glb_i1);
    *code_ptr = OPC_Halt;

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i0), 300, "i0");
    assert_equal(get_int(global_segment, glb_i1), 200, "i1");
}

// ---------------------------------------------------------------------
// TEST 2
// - load constants from const segment
// ---------------------------------------------------------------------

void test02(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_f1 = 0;
    const addr_t const_f2 = 8;
    set_float(memory->base, const_f1, 1000.125);
    set_float(memory->base, const_f2, 123.5);

    // globals
    const addr_t glb_f1 = 0;
    const addr_t glb_f2 = 8;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 1, const_f1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f1);
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 1, const_f2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f2);
    *code_ptr = OPC_Halt;

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_float(global_segment, glb_f1), 1000.125, "f1");
    assert_equal(get_float(global_segment, glb_f2), 123.5, "f2");
}

// ---------------------------------------------------------------------
// TEST 3
// - load and store globals
// - more integer arithmetics
// ---------------------------------------------------------------------

void test03(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;

    // code
    byte_t *code_ptr = code;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 100);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 30);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i2);
    code_ptr += emit_reg_addr(code_ptr, OPC_LdGlb_i32, 1, glb_i1);
    code_ptr += emit_reg_addr(code_ptr, OPC_LdGlb_i32, 2, glb_i2);
    code_ptr += emit_reg3(code_ptr, OPC_Sub_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i1);
    code_ptr += emit_reg3(code_ptr, OPC_Mul_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i2);
    code_ptr += emit_reg3(code_ptr, OPC_Div_i32, 3, 1, 2);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i3);
    *code_ptr = OPC_Halt;

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 70, "i1");
    assert_equal(get_int(global_segment, glb_i2), 3000, "i2");
    assert_equal(get_int(global_segment, glb_i3), 3, "i3");
}

// ---------------------------------------------------------------------
// TEST 4
// - branching, loops
// ---------------------------------------------------------------------

void test04(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;

    // code
    byte_t *code_ptr = code;
    // r1 <- 0
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0);
    // do r1 <- r1 + 1 while r1 != 100
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 100);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 3, 1);
    addr_t label_loop = code_ptr - code;
    code_ptr += emit_reg3(code_ptr, OPC_Add_i32, 1, 1, 3);
    code_ptr += emit_reg3(code_ptr, OPC_Eq_i32, 4, 1, 2);
    code_ptr += emit_reg_int(code_ptr, OPC_Br_zero, 4, label_loop);
    // i1 <- r1
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    *code_ptr = OPC_Halt;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 100, "i1");
}

// ---------------------------------------------------------------------
// TEST 5
// - boolean operators, mov
// ---------------------------------------------------------------------

void test05(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;

    // code
    byte_t *code_ptr = code;
    // r3 <- true and false
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, true);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, false);
    code_ptr += emit_reg3(code_ptr, OPC_And, 3, 1, 2);
    // r4 <- true or false
    code_ptr += emit_reg3(code_ptr, OPC_Or, 4, 1, 2);
    // i1 <- r3
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i1);
    // i2 <- r4
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 4, glb_i2);
    // r3 <- r4
    code_ptr += emit_reg2(code_ptr, OPC_Mov, 3, 4);
    // i3 <- r3
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i3);
    *code_ptr = OPC_Halt;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    config.debug_callback = dump_cpu;
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), false, "i1");
    assert_equal(get_int(global_segment, glb_i2), true, "i2");
    assert_equal(get_int(global_segment, glb_i3), true, "i3");
}

// ---------------------------------------------------------------------
// TEST 6
// - type conversion
// ---------------------------------------------------------------------

void test06(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_f1 = 0;
    set_float(memory->base, const_f1, 1000.125);

    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;
    const addr_t glb_f1 = 12;

    // code
    byte_t *code_ptr = code;
    // i1 <- (u8)0x123ab
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x123ab);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Conv_i32, 1, 1, TYPE_Unsigned8);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    // f1 <- (f64)0x123ab
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 123);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Conv_i32, 1, 1, TYPE_Float64);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 1, glb_f1);
    // i2 <- (i32)1000.125
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_f64, 1, const_f1);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Conv_f64, 1, 1, TYPE_Int32);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i2);
    // i3 <- (ref)0x123ab
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x123ab);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Conv_i32, 1, 1, TYPE_Ref);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_ref, 1, glb_i3);
    *code_ptr = OPC_Halt;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 0xab, "i1");
    assert_equal(get_float(global_segment, glb_f1), 123.0, "f1");
    assert_equal(get_int(global_segment, glb_i2), 1000, "i2");
    assert_equal(get_addr(global_segment, glb_i3), 0x123ab, "i3");
}

// ---------------------------------------------------------------------
// TEST 7
// - array allocation and element access
// ---------------------------------------------------------------------

void test07(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;
    const addr_t glb_i3 = 8;

    // code
    byte_t *code_ptr = code;
    // i1 <- new int[10]
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 10);
    code_ptr += emit_reg2(code_ptr, OPC_NewArr_i32, 2, 1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 2, glb_i1);
    // i2 <- new byte[20]
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 20);
    code_ptr += emit_reg2(code_ptr, OPC_NewArr_u8, 3, 1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i2);
    // i1[5] <- 123
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_i32, 1, 5);
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_i32, 4, 123);
    code_ptr += emit_reg3(code_ptr, OPC_StElem_i32, 4, 2, 1);
    // i3 <- i1[5]
    code_ptr += emit_reg3(code_ptr, OPC_LdElem_i32, 4, 2, 1);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 4, glb_i3);
    *code_ptr = OPC_Halt;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    const byte_t *heap_segment = heap_segment_addr(memory);
    const HeapEntry *entry1 = (HeapEntry *) (heap_segment + get_int(global_segment, glb_i1));
    assert_equal(entry1->header, TYPE_Int32, "heap_entry_1.header");
    assert_equal(entry1->data_size, 40, "heap_entry_1.data_size");
    assert_equal(entry1->ref_count, 0, "heap_entry_1.refcount");
    const HeapEntry *entry2 = (HeapEntry *) (heap_segment + get_int(global_segment, glb_i2));
    assert_equal(entry2->header, TYPE_Unsigned8, "heap_entry_2.header");
    assert_equal(entry2->data_size, 20, "heap_entry_2.data_size");
    assert_equal(entry2->ref_count, 0, "heap_entry_2.refcount");
    assert_equal(get_int(global_segment, glb_i3), 123, "i3");
}

// ---------------------------------------------------------------------
// TEST 8
// - struct allocation and field access
// ---------------------------------------------------------------------

struct test08_struct {
    // field       offset
    int32_t i;  // 0
    addr_t ref; // 4
    double f;   // 8
    byte_t b;   // 16
};

void test08(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_f1 = 0;
    set_float(memory->base, const_f1, 1000.125);
    const addr_t const_ts = 8;
    TypeMeta *test08_meta = (TypeMeta *) &memory->base[const_ts];
    test08_meta->name_offset = 0;
    strcpy((char *) test08_meta->data, "test08_struct");
    test08_meta->field_types_offset = strlen((char *) test08_meta->data);
    Type *field_types = &test08_meta->data[test08_meta->field_types_offset];
    field_types[0] = TYPE_Int32;
    field_types[1] = TYPE_Ref;
    field_types[2] = TYPE_Float64;
    field_types[3] = TYPE_Unsigned8;
    field_types[4] = TYPE_Void; // zero-terminate

    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_ref1 = 4;
    const addr_t glb_f1 = 8;
    const addr_t glb_b1 = 16;
    const addr_t glb_ts1 = 17;

    // code
    byte_t *code_ptr = code;
    // r1 <- new test08_struct{}
    code_ptr += emit_reg_int(code_ptr, OPC_NewObj, 1, const_ts);
    // glb_ts1 <- r1
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_ref, 1, glb_ts1);
    // r1.i <- 123
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 123);
    code_ptr += emit_reg2_addr(code_ptr, OPC_StFld_i32, 2, 1, 0);
    // r1.ref <- 234
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 234);
    code_ptr += emit_reg2_addr(code_ptr, OPC_StFld_ref, 2, 1, 4);
    // r1.f <- 1000.125
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_f64, 2, const_f1);
    code_ptr += emit_reg2_addr(code_ptr, OPC_StFld_f64, 2, 1, 8);
    // r1.b <- 255
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 255);
    code_ptr += emit_reg2_addr(code_ptr, OPC_StFld_u8, 2, 1, 16);
    // i1 <- r1.i
    code_ptr += emit_reg2_addr(code_ptr, OPC_LdFld_i32, 3, 1, 0);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 3, glb_i1);
    // ref1 <- r1.ref
    code_ptr += emit_reg2_addr(code_ptr, OPC_LdFld_ref, 3, 1, 4);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_ref, 3, glb_ref1);
    // f1 <- r1.f
    code_ptr += emit_reg2_addr(code_ptr, OPC_LdFld_f64, 3, 1, 8);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 3, glb_f1);
    // b1 <- r1.b
    code_ptr += emit_reg2_addr(code_ptr, OPC_LdFld_u8, 3, 1, 16);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_u8, 3, glb_b1);
    *code_ptr = OPC_Halt;
    print_code(stdout, code, code_ptr - code + 1);

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    const byte_t *heap_segment = heap_segment_addr(memory);
    const struct test08_struct expected_struct = {
            .i = 123,
            .ref = 234,
            .f = 1000.125,
            .b = 255
    };
    int struct_comparison = memcmp(&expected_struct,
                                   &heap_segment[get_addr(global_segment, glb_ts1) + HEAP_ENTRY_HEADER_SIZE],
                                   sizeof_instance(test08_meta));
    assert_equal(struct_comparison, 0, "test08_struct comparison");
    assert_equal(get_int(global_segment, glb_i1), 123, "i1");
    assert_equal(get_float(global_segment, glb_f1), 1000.125, "f1");
    assert_equal(get_addr(global_segment, glb_ref1), 234, "ref1");
    assert_equal(get_byte(global_segment, glb_b1), 255, "b1");
}

// ---------------------------------------------------------------------
// TEST 9
// - branching in multi-module program (within single module)
// ---------------------------------------------------------------------

void test09(byte_t *code, MemoryLayout *memory) {
    // globals
    const addr_t glb_i1 = 0;

    // code
    byte_t *code_ptr = code;
    // module 0: dummy code
    code_ptr += emit_reg(code_ptr, OPC_Ldc_zero, 0);
    *code_ptr++ = OPC_Nop;
    *code_ptr++ = OPC_Nop;
    *code_ptr++ = OPC_Nop;
    // module 1:
    // r1 <- 0
    const byte_t *module1_ptr = code_ptr;
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0);
     // while r1 < 100 do r1 <- r1 + 1
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 100);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 3, 1);
    addr_t label_loop = code_ptr - code;
    code_ptr += emit_reg3(code_ptr, OPC_Lt_i32, 4, 1, 2);
    Instruction *branch_instr = (Instruction *) code_ptr; // branch instr needs fixup later
    code_ptr += emit_reg_int(code_ptr, OPC_Br_zero, 4, 0);
    code_ptr += emit_reg3(code_ptr, OPC_Add_i32, 1, 1, 3);
    code_ptr += emit_addr(code_ptr, OPC_Br, label_loop);
    set_addr(branch_instr->args, 1, code_ptr - code); // fixup branch instr
    // i1 <- r1
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    *code_ptr = OPC_Halt;

    print_code(stdout, code, module1_ptr - code);
    print_code(stdout, module1_ptr, code_ptr - module1_ptr + 1);

    // act
    FunctionMeta entry_point;
    zero_memory(&entry_point, sizeof(entry_point));
    execute(code, &entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 100, "i1");
}

// ---------------------------------------------------------------------
// TEST 10
// - function call `function f()`
// ---------------------------------------------------------------------

void test10(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_func1 = 0;
    FunctionMeta *func_meta = (FunctionMeta *) memory->base;
    zero_memory(func_meta, sizeof(FunctionMeta));
    strcpy(func_meta->name, "func1");

    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_i2 = 4;

    // code
    byte_t *code_ptr = code;
    // func1()
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 7, 0x666);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Call, 0, 0, const_func1);
    // i1 <- 0x123
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x123);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    *code_ptr++ = OPC_Halt;
    // def func1:
    func_meta->pc = code_ptr - code;
    // i2 <- 0x234
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x234);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i2);
    *code_ptr++ = OPC_Ret;
    print_code(stdout, code, code_ptr - code);

    // act
    config.debug_callback = dump_cpu;
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 0x123, "i1");
    assert_equal(get_int(global_segment, glb_i2), 0x234, "i2");
}


// ---------------------------------------------------------------------
// TEST 11
// - function call `function f() -> int`
// ---------------------------------------------------------------------

void test11(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_func1 = 0;
    FunctionMeta *func_meta = (FunctionMeta *) memory->base;
    zero_memory(func_meta, sizeof(FunctionMeta));
    func_meta->ret_type = TYPE_Int32;
    strcpy(func_meta->name, "func1");

    // globals
    const addr_t glb_i1 = 0;

    // code
    byte_t *code_ptr = code;
    // r1 <- func1()
    code_ptr += emit_reg2_addr(code_ptr, OPC_Call, 1, 0, const_func1);
    // i1 <- r1
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    *code_ptr++ = OPC_Halt;
    // def func1:
    func_meta->pc = code_ptr - code;
    // ret 0x234
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 0, 0x123);
    *code_ptr++ = OPC_Ret;
    print_code(stdout, code, code_ptr - code);

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 0x123, "i1");
}

// ---------------------------------------------------------------------
// TEST 12
// - function call `function f(int, double) -> ref`
// ---------------------------------------------------------------------

void test12(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_f1 = 0;
    set_float(memory->base, 0, 1000.125);
    const addr_t const_func1 = 8;
    FunctionMeta *func_meta = (FunctionMeta *) &memory->base[const_func1];
    zero_memory(func_meta, sizeof(FunctionMeta));
    func_meta->ret_type = TYPE_Ref;
    func_meta->arg_count = 2;
    strcpy(func_meta->name, "func1");

    // globals
    const addr_t glb_i1 = 0;
    const addr_t glb_f1 = 4;

    // code
    byte_t *code_ptr = code;
    // r1 <- func1(123, 1000.125)
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x123);
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 2, const_f1);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Call, 1, 1, const_func1);
    // i1 <- r1
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_ref, 1, glb_i1);
    *code_ptr++ = OPC_Halt;
    // def func1:
    func_meta->pc = code_ptr - code;
    // ret r1 * 2
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 3, 2);
    code_ptr += emit_reg3(code_ptr, OPC_Mul_i32, 0, 1, 3);
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_f64, 2, glb_f1);
    *code_ptr++ = OPC_Ret;
    print_code(stdout, code, code_ptr - code);

    // act
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 0x123 * 2, "i1");
    assert_equal(get_float(global_segment, glb_f1), 1000.125, "f1");
}

// ---------------------------------------------------------------------
// TEST 13
// - nested function calls
//      `function f(int, double) -> int`
//      `function g(byte) -> int
//   across modules
// ---------------------------------------------------------------------

void test13(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_f1 = 0;
    set_float(memory->base, 0, 1000.125);
    // f(int, double) -> ref
    const addr_t const_func1 = 8;
    FunctionMeta *func1_meta = (FunctionMeta *) &memory->base[const_func1];
    zero_memory(func1_meta, sizeof(FunctionMeta));
    func1_meta->ret_type = TYPE_Int32;
    func1_meta->arg_count = 2;
    strcpy(func1_meta->name, "func1");
    // g(byte) -> int
    const addr_t const_func2 = 8 + sizeof(FunctionMeta);
    FunctionMeta *func2_meta = (FunctionMeta *) &memory->base[const_func2];
    zero_memory(func2_meta, sizeof(FunctionMeta));
    func2_meta->ret_type = TYPE_Int32;
    func2_meta->arg_count = 1;
    strcpy(func2_meta->name, "func2");

    /*
     * main() {
     *      glb_i1 = f(0x123, 1000.125)
     * }
     *
     * func1(i, d) {
     *      return func2((byte) i) + (int) d
     * }
     *
     * func2(b) {
     *      return b * 2
     * }
     */

    // globals
    const addr_t glb_i1 = 0;

    // code
    byte_t *code_ptr = code;
    // r1 <- func1(123, 1000.125)
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0x123);
    code_ptr += emit_reg_addr(code_ptr, OPC_Ldc_f64, 2, const_f1);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Call, 1, 1, const_func1);
    // i1 <- r1
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_i32, 1, glb_i1);
    *code_ptr++ = OPC_Halt;
    // def func1:
    func1_meta->pc = code_ptr - code;
    // r3 <- (byte) r1
    code_ptr += emit_reg2_addr(code_ptr, OPC_Conv_i32, 3, 1, TYPE_Unsigned8);
    // r3 <- g(r3)
    code_ptr += emit_reg2_addr(code_ptr, OPC_Call, 3, 3, const_func2);
    // r4 <- (int) r2
    code_ptr += emit_reg2_addr(code_ptr, OPC_Conv_f64, 4, 2, TYPE_Int32);
    // ret r3 + r4
    code_ptr += emit_reg3(code_ptr, OPC_Add_i32, 0, 3, 4);
    *code_ptr++ = OPC_Ret;
    // def func2:
    func2_meta->pc = code_ptr - code;
    *code_ptr++ = OPC_Nop;
    // ret r1 * 2
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 0x2);
    code_ptr += emit_reg3(code_ptr, OPC_Mul_i32, 0, 1, 2);
    *code_ptr++ = OPC_Ret;
    print_code(stdout, code, code_ptr - code);

    // act
    config.debug_callback = dump_cpu;
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 0x46 + 1000, "i1");
}

// ---------------------------------------------------------------------
// TEST 14
// - recursive function call `function f(int) -> int`
// ---------------------------------------------------------------------

void test14(byte_t *code, MemoryLayout *memory) {
    // constants
    const addr_t const_func1 = 0;
    FunctionMeta *func_meta = (FunctionMeta *) &memory->base[const_func1];
    zero_memory(func_meta, sizeof(FunctionMeta));
    func_meta->ret_type = TYPE_Int32;
    func_meta->arg_count = 1;
    strcpy(func_meta->name, "func1");

    /*
     * main() {
     *      glb_i1 = func1(0)
     * }
     *
     * func1(i) -> int {
     *      if i < 10 {
     *          return func1(i + 1)
     *      }
     *      return i
     * }
     */

    // globals
    const addr_t glb_i1 = 0;

    // code
    byte_t *code_ptr = code;
    // r1 <- func1(0)
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 1, 0);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Call, 1, 1, const_func1);
    // i1 <- r1
    code_ptr += emit_reg_addr(code_ptr, OPC_StGlb_ref, 1, glb_i1);
    *code_ptr++ = OPC_Halt;
    // def func1:
    func_meta->pc = code_ptr - code;
    // ret r1 * 2
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 1);
    code_ptr += emit_reg3(code_ptr, OPC_Add_i32, 0, 1, 2);
    code_ptr += emit_reg_int(code_ptr, OPC_Ldc_i32, 2, 10);
    code_ptr += emit_reg3(code_ptr, OPC_Lt_i32, 2, 0, 2);
    Instruction *branch_instr = (Instruction *) code_ptr; // branch instr needs fixup later
    code_ptr += emit_reg_addr(code_ptr, OPC_Br_zero, 2, 0);
    code_ptr += emit_reg2_addr(code_ptr, OPC_Call, 0, 0, const_func1);
    set_addr(branch_instr->args, 1, code_ptr - code); // fixup branch instr
    *code_ptr++ = OPC_Ret;
    print_code(stdout, code, code_ptr - code);

    // act
    config.debug_callback = dump_cpu;
    execute(code, &default_entry_point, memory, &config);

    // assert
    const byte_t *global_segment = global_segment_addr(memory);
    assert_equal(get_int(global_segment, glb_i1), 10, "i1");
}

// ---------------------------------------------------------------------
// TEST 15
// - object allocation
// - reference counting
// - object de-allocation
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 16
// - heap compaction
// ---------------------------------------------------------------------

// ---------------------------------------------------------------------
// TEST 17
// - strings
// ---------------------------------------------------------------------


// ---------------------------------------------------------------------
// TEST entry point
// ---------------------------------------------------------------------

static const struct test {
    void (*proc)(byte_t *, MemoryLayout *);
    const char *name;
} tests[] = {
        { .proc = test01, .name = "load integer constants, add integers, store globals" },
        { .proc = test02, .name = "load f64 constant" },
        { .proc = test03, .name = "load and store globals, more integer arithmetic" },
        { .proc = test04, .name = "branching and loops" },
        { .proc = test05, .name = "boolean operators, mov" },
        { .proc = test06, .name = "type conversion" },
        { .proc = test07, .name = "array allocation and element access" },
        { .proc = test08, .name = "struct allocation and field access" },
        { .proc = test09, .name = "branching in multi-module program" },
        { .proc = test10, .name = "call f()" },
        { .proc = test11, .name = "call f() -> int" },
        { .proc = test12, .name = "call f(int, double) -> ref" },
        { .proc = test13, .name = "nested function calls" },
        { .proc = test14, .name = "recursive function call" },
        { .proc = NULL },
};

static byte_t memory_segment[64 * 1024];
static byte_t code_segment[1024];

int main() {
    MemoryLayout memory;
    fprintf(stdout, "zln_test v%d.%d\n", zln_VERSION_MAJOR, zln_VERSION_MINOR);
    fprintf(stdout, "RemoveRef=%d\n", OPC_RemoveRef);
    fprintf(stdout, ">>> running tests...\n");
    int n = 1;

    for (const struct test *test_ptr = tests; test_ptr->proc != NULL; test_ptr++, n++) {
        fprintf(stdout, "%d) %s\n", n, test_ptr->name);
        zero_memory(memory_segment, sizeof(memory_segment));
        zero_memory(code_segment, sizeof(code_segment));
        zero_memory(&memory, sizeof(memory));
        memory.base = memory_segment;
        memory.total_size = sizeof(memory_segment);
        memory.const_segment_size = 1024;
        memory.global_segment_size = 1024;
        memory.register_segment_size = config.register_count * config.max_stack_depth * sizeof(Register);
        memory.stack_frame_segment_size = config.max_stack_depth * sizeof(StackFrame);
        test_ptr->proc(code_segment, &memory);
        config.debug_callback = NULL;
    }

    fprintf(stdout, "<<< tests completed successfully\n");
    return 0;
}
