package net.smackem.zlang.emit.ir;

import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.SourceFileLocations;
import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.ProgramStructure;
import net.smackem.zlang.symbols.SymbolExtractor;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EmitterTest {
    @Test
    public void basics() throws CompilationErrorException, IOException {
        final List<ParsedModule> modules = parseModule("""
                struct Mutable {
                    fld1: float
                }
                let glb1: float = 12.5
                let glb2: Mutable = nil
                let glb3: int[] = nil
                fn main() {
                    var x: int = 12
                    var y: int = 23
                    var z: int = x + y
                    glb2.fld1 = 25.25
                    glb3[100] = x
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final Program program = Emitter.emit(ps, modules);
        assertThat(program.instructions()).isNotEmpty();
        final StringBuilder sb = new StringBuilder();
        int index = 0;
        for (final Instruction instr : program.instructions()) {
            sb.append(String.format("%02d ", index));
            sb.append(instr).append(System.lineSeparator());
            index++;
        }
        System.out.println(sb);
    }

    private static List<ParsedModule> parseModule(String source) throws IOException, CompilationErrorException {
        return List.of(ParsedModule.parse("main",
                SourceFileLocations.ofMap(Map.of("main", source))));
    }
}
