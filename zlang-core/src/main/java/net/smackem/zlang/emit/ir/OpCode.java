package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.BuiltInTypeSymbol;
import net.smackem.zlang.symbols.Type;

import java.util.Map;

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

    private static final Map<BuiltInTypeSymbol, OpCode> ldGlb = Map.of(
            BuiltInTypeSymbol.INT, LdGlb_i32,
            BuiltInTypeSymbol.FLOAT, LdGlb_f64,
            BuiltInTypeSymbol.BYTE, LdGlb_u8,
            BuiltInTypeSymbol.OBJECT, LdGlb_ref,
            BuiltInTypeSymbol.RUNTIME_PTR, LdGlb_ptr);

    public static OpCode ldGlb(Type type) {
        return ldGlb.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> ldFld = Map.of(
            BuiltInTypeSymbol.INT, LdFld_i32,
            BuiltInTypeSymbol.FLOAT, LdFld_f64,
            BuiltInTypeSymbol.BYTE, LdFld_u8,
            BuiltInTypeSymbol.OBJECT, LdFld_ref,
            BuiltInTypeSymbol.RUNTIME_PTR, LdFld_ptr);

    public static OpCode ldFld(Type type) {
        return ldFld.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> ldElem = Map.of(
            BuiltInTypeSymbol.INT, LdElem_i32,
            BuiltInTypeSymbol.FLOAT, LdElem_f64,
            BuiltInTypeSymbol.BYTE, LdElem_u8,
            BuiltInTypeSymbol.OBJECT, LdElem_ref,
            BuiltInTypeSymbol.RUNTIME_PTR, LdElem_ptr);

    public static OpCode ldElem(Type type) {
        return ldElem.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> stGlb = Map.of(
            BuiltInTypeSymbol.INT, StGlb_i32,
            BuiltInTypeSymbol.FLOAT, StGlb_f64,
            BuiltInTypeSymbol.BYTE, StGlb_u8,
            BuiltInTypeSymbol.OBJECT, StGlb_ref,
            BuiltInTypeSymbol.RUNTIME_PTR, StGlb_ptr);

    public static OpCode stGlb(Type type) {
        return stGlb.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> stFld = Map.of(
            BuiltInTypeSymbol.INT, StFld_i32,
            BuiltInTypeSymbol.FLOAT, StFld_f64,
            BuiltInTypeSymbol.BYTE, StFld_u8,
            BuiltInTypeSymbol.OBJECT, StFld_ref,
            BuiltInTypeSymbol.RUNTIME_PTR, StFld_ptr);

    public static OpCode stFld(Type type) {
        return stFld.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> stElem = Map.of(
            BuiltInTypeSymbol.INT, StElem_i32,
            BuiltInTypeSymbol.FLOAT, StElem_f64,
            BuiltInTypeSymbol.BYTE, StElem_u8,
            BuiltInTypeSymbol.OBJECT, StElem_ref,
            BuiltInTypeSymbol.RUNTIME_PTR, StElem_ptr);

    public static OpCode stElem(Type type) {
        return stElem.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> ldc = Map.of(
            BuiltInTypeSymbol.INT, Ldc_i32,
            BuiltInTypeSymbol.FLOAT, Ldc_f64,
            BuiltInTypeSymbol.OBJECT, Ldc_ref);

    public static OpCode ldc(Type type) {
        return ldc.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> add = Map.of(
            BuiltInTypeSymbol.INT, Add_i32,
            BuiltInTypeSymbol.FLOAT, Add_f64,
            BuiltInTypeSymbol.BYTE, Add_u8,
            BuiltInTypeSymbol.STRING, Add_str);

    public static OpCode add(Type type) {
        return add.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> sub = Map.of(
            BuiltInTypeSymbol.INT, Sub_i32,
            BuiltInTypeSymbol.FLOAT, Sub_f64,
            BuiltInTypeSymbol.BYTE, Sub_u8);

    public static OpCode sub(Type type) {
        return sub.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> mul = Map.of(
            BuiltInTypeSymbol.INT, Mul_i32,
            BuiltInTypeSymbol.FLOAT, Mul_f64,
            BuiltInTypeSymbol.BYTE, Mul_u8);

    public static OpCode mul(Type type) {
        return mul.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> div = Map.of(
            BuiltInTypeSymbol.INT, Div_i32,
            BuiltInTypeSymbol.FLOAT, Div_f64,
            BuiltInTypeSymbol.BYTE, Div_u8);

    public static OpCode div(Type type) {
        return div.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> eq = Map.of(
            BuiltInTypeSymbol.INT, Eq_i32,
            BuiltInTypeSymbol.FLOAT, Eq_f64,
            BuiltInTypeSymbol.BYTE, Eq_u8,
            BuiltInTypeSymbol.OBJECT, Eq_ref,
            BuiltInTypeSymbol.STRING, Eq_str,
            BuiltInTypeSymbol.RUNTIME_PTR, Eq_ptr);

    public static OpCode eq(Type type) {
        return eq.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> ne = Map.of(
            BuiltInTypeSymbol.INT, Ne_i32,
            BuiltInTypeSymbol.FLOAT, Ne_f64,
            BuiltInTypeSymbol.BYTE, Ne_u8,
            BuiltInTypeSymbol.OBJECT, Ne_ref,
            BuiltInTypeSymbol.STRING, Ne_str,
            BuiltInTypeSymbol.RUNTIME_PTR, Ne_ptr);

    public static OpCode ne(Type type) {
        return ne.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> gt = Map.of(
            BuiltInTypeSymbol.INT, Gt_i32,
            BuiltInTypeSymbol.FLOAT, Gt_f64,
            BuiltInTypeSymbol.BYTE, Gt_u8,
            BuiltInTypeSymbol.STRING, Gt_str);

    public static OpCode gt(Type type) {
        return gt.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> ge = Map.of(
            BuiltInTypeSymbol.INT, Ge_i32,
            BuiltInTypeSymbol.FLOAT, Ge_f64,
            BuiltInTypeSymbol.BYTE, Ge_u8,
            BuiltInTypeSymbol.STRING, Ge_str);

    public static OpCode ge(Type type) {
        return ge.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> lt = Map.of(
            BuiltInTypeSymbol.INT, Lt_i32,
            BuiltInTypeSymbol.FLOAT, Lt_f64,
            BuiltInTypeSymbol.BYTE, Lt_u8,
            BuiltInTypeSymbol.STRING, Lt_str);

    public static OpCode lt(Type type) {
        return lt.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> le = Map.of(
            BuiltInTypeSymbol.INT, Le_i32,
            BuiltInTypeSymbol.FLOAT, Le_f64,
            BuiltInTypeSymbol.BYTE, Le_u8,
            BuiltInTypeSymbol.STRING, Le_str);

    public static OpCode le(Type type) {
        return le.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> conv = Map.of(
            BuiltInTypeSymbol.INT, Conv_i32,
            BuiltInTypeSymbol.FLOAT, Conv_f64,
            BuiltInTypeSymbol.BYTE, Conv_u8,
            BuiltInTypeSymbol.OBJECT, Conv_ref,
            BuiltInTypeSymbol.STRING, Conv_str,
            BuiltInTypeSymbol.RUNTIME_PTR, Conv_ptr);

    public static OpCode conv(Type type) {
        return conv.get(type.primitive());
    }

    private static final Map<BuiltInTypeSymbol, OpCode> newArr = Map.of(
            BuiltInTypeSymbol.INT, NewArr_i32,
            BuiltInTypeSymbol.FLOAT, NewArr_f64,
            BuiltInTypeSymbol.BYTE, NewArr_u8,
            BuiltInTypeSymbol.OBJECT, NewArr_ref,
            BuiltInTypeSymbol.RUNTIME_PTR, NewArr_ptr);

    public static OpCode newArr(Type type) {
        return newArr.get(type.primitive());
    }
}
