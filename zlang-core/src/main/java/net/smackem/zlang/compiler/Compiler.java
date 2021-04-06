package net.smackem.zlang.compiler;

import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.symbols.Symbol;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Collection;
import java.util.Map;

public class Compiler {
    public void compile(ParsedModule dt) {
    }

    Map<ParserRuleContext, Symbol> extractSymbols(ParsedModule dt) {
        final Collection<ParsedModule> sources = dt.flatten();
        for (final ParsedModule source : sources) {
        }
        return null;
    }
}
