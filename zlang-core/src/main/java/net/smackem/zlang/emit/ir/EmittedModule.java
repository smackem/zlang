package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.Type;

import java.util.ArrayList;
import java.util.List;

public class EmittedModule {
    private final List<Type> types = new ArrayList<>();
    private final List<EmittedFunction> functions = new ArrayList<>();
}
