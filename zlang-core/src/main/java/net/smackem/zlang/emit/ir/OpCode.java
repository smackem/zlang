package net.smackem.zlang.emit.ir;

public enum OpCode {
    /**
     * nop():
     */
    Nop(0),

    /**
     * load_global(REG r_target, INT glb_addr):
     *      r_target <- *glb_addr
     * glb_addr is relative to global segment base
     */
    LdGlb_i32(1),
    LdGlb_f64(2),
    LdGlb_u8(3),
    LdGlb_ref(4),
    LdGlb_ptr(5),

    /**
     * load_field(REG r_target, REG r_heap_addr, INT field_offset):
     *      r_target <- *(r_heap_addr + field_offset)
     * r_heap_addr is relative to heap base
     */
    LdFld_i32(6),
    LdFld_f64(7),
    LdFld_u8(8),
    LdFld_ref(9),
    LdFld_ptr(10),

    /**
     * load_array_element(REG r_target, REG r_heap_addr, REG r_elem_offset):
     *      r_target <- *(r_heap_addr + r_elem_offset)
     * r_heap_addr is relative to heap base. r_elem_offset is a byte offset.
     */
    LdElem_i32(11),
    LdElem_f64(12),
    LdElem_u8(13),
    LdElem_ref(14),
    LdElem_ptr(15),

    /**
     * store_global(REG r_source, INT glb_addr):
     *      r_source -> *glb_addr
     * glb_addr is relative to global base
     */
    StGlb_i32(16),
    StGlb_f64(17),
    StGlb_u8(18),
    StGlb_ref(19),
    StGlb_ptr(20),

    /**
     * store_field(REG r_source, REG r_heap_addr, INT field_offset):
     *      r_source -> *(r_heap_addr + field_offset)
     * r_heap_addr is relative to heap base
     */
    StFld_i32(21),
    StFld_f64(22),
    StFld_u8(23),
    StFld_ref(24),
    StFld_ptr(25),

    /**
     * store_array_element(REG r_source, REG r_heap_addr, REG r_elem_offset):
     *      r_source -> *(r_heap_addr + r_elem_offset)
     * r_heap_addr is relative to heap base. r_elem_offset is a byte offset.
     */
    StElem_i32(26),
    StElem_f64(27),
    StElem_u8(28),
    StElem_ref(29),
    StElem_ptr(30),

    /**
     * load_immediate_constant(REG r_target, INT value):
     *      r_target <- value
     */
    Ldc_i32(31),
    Ldc_ref(32),
    /**
     * load_constant(REG r_target, INT const_addr):
     *      r_target <- *const_addr
     * const_addr is relative to const base
     */
    Ldc_f64(33),
    /**
     * load_constant_zero(REG r_target):
     *      r_target <- 0 (zero entire register)
     *      works for all data types
     */
    Ldc_zero(34),

    /**
     * add(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left + r_right
     */
    Add_i32(35),
    Add_f64(36),
    Add_u8(37),
    Add_str(38),

    /**
     * sub(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left - r_right
     */
    Sub_i32(39),
    Sub_f64(40),
    Sub_u8(41),

    /**
     * mul(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left * r_right
     */
    Mul_i32(42),
    Mul_f64(43),
    Mul_u8(44),

    /**
     * div(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left / r_right
     */
    Div_i32(45),
    Div_f64(46),
    Div_u8(47),

    /**
     * equals(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left == r_right
     */
    Eq_i32(48),
    Eq_f64(49),
    Eq_u8(50),
    Eq_str(51),
    Eq_ref(52),
    Eq_ptr(53),

    /**
     * not_equals(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left != r_right
     */
    Ne_i32(54),
    Ne_f64(55),
    Ne_u8(56),
    Ne_str(57),
    Ne_ref(58),
    Ne_ptr(59),

    /**
     * greater_than(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left > r_right
     */
    Gt_i32(60),
    Gt_f64(61),
    Gt_u8(62),
    Gt_str(63),

    /**
     * greater_than_or_equal(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left >= r_right
     */
    Ge_i32(64),
    Ge_f64(65),
    Ge_u8(66),
    Ge_str(67),

    /**
     * less_than(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left > r_right
     */
    Lt_i32(68),
    Lt_f64(69),
    Lt_u8(70),
    Lt_str(71),

    /**
     * less_than_or_equal(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left >= r_right
     */
    Le_i32(72),
    Le_f64(73),
    Le_u8(74),
    Le_str(75),

    /**
     * and(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left and r_right
     */
    And(76),

    /**
     * or(REG r_target, REG r_left, REG r_right):
     *      r_target <- r_left or r_right
     */
    Or(77),

    /**
     * move(REG r_target, REG r_source):
     *      r_target <- r_source
     */
    Mov(78),

    /**
     * branch_if_false(REG r_source, INT new_pc):
     *      if r_source.i32 != 0:
     *          pc <- new_pc
     * new_pc is relative to base_pc
     */
    Br_zero(79),

    /**
     * branch(INT new_pc):
     *      pc <- new_pc
     * new_pc is relative to base_pc
     */
    Br(80),

    /**
     * call(REG r_target, REG r_first_arg, INT const_addr):
     *      (look at FunctionMeta at const_addr)
     *      push stack_frame(#r_target, base_pc, pc, FunctionMeta)
     *      copy arguments: registers r_first_arg..r_first_arg + FunctionMeta.arg_count
     *              to new stack frame
     *      base_pc <- FunctionMeta.base_pc
     *      pc <- FunctionMeta.pc
     */
    Call(81),

    /**
     * return():
     *      pop stack_frame sf
     *      base_pc <- sf.base_pc
     *      pc <- sf.pc + 1
     *      save r0 to ret_val
     *      pop base_registers
     *      sf.r_target <- ret_val
     */
    Ret(82),

    /**
     * halt():
     *      stop program execution
     */
    Halt(83),

    /**
     * convert(REG r_target, REG r_source, TYPE target_type):
     *      r_target <- r_source converted to target_type (see ::Type)
     */
    Conv_i32(84),
    Conv_f64(85),
    Conv_u8(86),
    Conv_str(87),
    Conv_ref(88),
    Conv_ptr(89),

    /**
     * new_object(REG r_target, INT const_addr):
     *      (look at type_meta at const_addr)
     *      allocate memory on heap for new instance of type_meta
     *      r_target <- heap_addr of new memory block on heap (TYPE_REF)
     * const_addr is relative to const base
     */
    NewObj(90),

    /**
     * new_string(REG r_target, INT data_size):
     *      allocate memory on heap for data_size characters
     *      r_target <- addr of new memory block on heap (TYPE_REF)
     */
    NewStr(91),

    /**
     * new_array(REG r_target, REG r_size):
     *      allocate memory on heap for *r_size items
     *      r_target <- heap_addr of new memory block on heap (TYPE_REF)
     */
    NewArr_i32(92),
    NewArr_f64(93),
    NewArr_u8(94),
    NewArr_ref(95),
    NewArr_ptr(96),

    /**
     * add_reference(REG r_heap_addr):
     *      increment reference count for object at r_heap_addr
     */
    AddRef(97),

    /**
     * remove_reference(REG r_heap_addr):
     *      decrement reference count for object at r_heap_addr
     */
    RemoveRef(98);

    private final int code;

    OpCode(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }
}
