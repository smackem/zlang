package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.Type;
import net.smackem.zlang.symbols.VariableSymbol;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record Program(List<Instruction> instructions,
                      Collection<Type> types,
                      Collection<VariableSymbol> globals,
                      Map<FunctionSymbol, Instruction> codeMap,
                      FunctionSymbol entryPoint,
                      Instruction entryPointBaseInstruction,
                      Collection<Label> labels) {
    Program freeze() {
        return new Program(Collections.unmodifiableList(this.instructions),
                Collections.unmodifiableCollection(this.types),
                Collections.unmodifiableCollection(this.globals),
                Collections.unmodifiableMap(this.codeMap),
                this.entryPoint,
                this.entryPointBaseInstruction,
                Collections.unmodifiableCollection(this.labels));
    }
}
