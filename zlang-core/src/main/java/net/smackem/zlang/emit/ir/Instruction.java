package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.Symbol;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class Instruction {
    private final OpCode opCode;
    private final Register[] registerArgs = new Register[3];
    private Symbol symbolArg;
    private long intArg;
    private Label labelArg;
    private double floatArg;
    private String strArg;
    private int address;

    Instruction(OpCode opCode) {
        this.opCode = Objects.requireNonNull(opCode);
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

    public String strArg() {
        return this.strArg;
    }

    void setStrArg(String str) {
        this.strArg = str;
    }

    public int address() {
        return this.address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(String.format("%08x ", this.address));
        sb.append(this.opCode.toString()).append(" ");
        sb.append(Arrays.stream(this.registerArgs)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining(" ")));
        sb.append(" int=").append(intArg);
        sb.append(" float=").append(floatArg);
        if (symbolArg != null) {
            sb.append(" symbol=").append(symbolArg);
        }
        if (labelArg != null) {
            sb.append(" symbol=").append(labelArg);
        }
        if (strArg != null) {
            sb.append(" symbol=").append(strArg);
        }
        return sb.toString();
    }
}
