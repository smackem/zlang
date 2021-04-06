package net.smackem.zlang.emit.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Label {
    private Instruction target;
    private final List<Instruction> sources = new ArrayList<>();

    Label(Instruction target, Instruction source) {
        this.target = target;
        this.sources.add(source);
    }

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
}
