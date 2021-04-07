package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.Symbol;

public class Instruction {
    private final OpCode opCode;
    private final int[] registerArgs = new int[3];
    private Symbol symbolArg;
    private int intArg;

    Instruction(OpCode opCode) {
        this.opCode = opCode;
    }

    public OpCode opCode() {
        return this.opCode;
    }

    public int registerArg(int index) {
        return this.registerArgs[index];
    }

    void setRegisterArg(int index, int register) {
        this.registerArgs[index] = register;
    }

    public Symbol symbolArg() {
        return this.symbolArg;
    }

    void setSymbolArg(Symbol symbol) {
        this.symbolArg = symbol;
    }

    public int intArg() {
        return this.intArg;
    }

    void setIntArg(int integer) {
        this.intArg = integer;
    }
}
