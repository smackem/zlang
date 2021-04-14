package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.FunctionSymbol;
import net.smackem.zlang.symbols.Type;

import java.util.List;
import java.util.Map;

public record Program(List<Instruction> instructions,
                      List<Type> types,
                      Map<FunctionSymbol, Instruction> codeMap,
                      FunctionSymbol entryPoint,
                      Instruction entryPointBaseInstruction) {
}
