package net.smackem.zlang.emit.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Label {
    private Instruction target;
    private final List<Instruction> sources = new ArrayList<>();

    Label() { }

    public Collection<Instruction> sources() {
        return Collections.unmodifiableCollection(this.sources);
    }

    public void addSource(Instruction source) {
        this.sources.add(source);
    }

    public Instruction target() {
        return this.target;
    }

    void setTarget(Instruction target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "Label{" +
                "target=" + (target != null ? target.opCode() : "(null)") +
                ", sources=" + (sources.stream().map(Instruction::opCode).map(Object::toString).collect(Collectors.joining(", "))) +
                '}';
    }
}
