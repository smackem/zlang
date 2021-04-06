package net.smackem.zlang.emit.ir;

import java.util.List;

public class Instruction {
    private final OpCode opCode;
    private final List<Object> arguments;

    Instruction(OpCode opCode, Object... arguments) {
        this.opCode = opCode;
        this.arguments = List.of(arguments);
    }

    public OpCode opCode() {
        return this.opCode;
    }

    public List<Object> arguments() {
        return this.arguments;
    }
}
