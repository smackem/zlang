package net.smackem.zlang.emit.ir;

import java.util.Collection;

public class Instructions {
    private Instructions() { }

    public static String print(Collection<Instruction> instructions) {
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        for (final Instruction instr : instructions) {
            sb.append(String.format("%02d ", index));
            sb.append(instr).append(System.lineSeparator());
            index++;
        }
        return sb.toString();
    }
}
