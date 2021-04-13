package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.Type;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record EmittedModule(List<Type> types,
                            List<FunctionSymbol> functions,
                            List<Instruction> instructions,
                            Map<FunctionSymbol, Instruction> codeMap) {
}
