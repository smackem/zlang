package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.Symbol;

public class Instruction {
    private final OpCode opCode;
    private final Register[] registerArgs = new Register[3];
    private Symbol symbolArg;
    private long intArg;
    private Label labelArg;
    private double floatArg;

    Instruction(OpCode opCode) {
        this.opCode = opCode;
    }

    public OpCode opCode() {
        return this.opCode;
    }

    public Register registerArg(int index) {
        return this.registerArgs[index];
    }

    void setRegisterArg(int index, Register register) {
        this.registerArgs[index] = register;
    }

    public Symbol symbolArg() {
        return this.symbolArg;
    }

    void setSymbolArg(Symbol symbol) {
        this.symbolArg = symbol;
    }

    public long intArg() {
        return this.intArg;
    }

    void setIntArg(long integer) {
        this.intArg = integer;
    }

    public Label labelArg() {
        return this.labelArg;
    }

    void setLabelArg(Label label) {
        this.labelArg = label;
    }

    public double floatArg() {
        return this.floatArg;
    }

    void setFloatArg(double floatArg) {
        this.floatArg = floatArg;
    }
}
