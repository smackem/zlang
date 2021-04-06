package net.smackem.zlang.symbols;

import net.smackem.zlang.modules.ParsedModule;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SymbolExtractor {

    /**
     * Extracts all scopes and their contained symbols from the the specified modules.
     *
     * @param modules
     *      The flattened list of modules that build a dependency tree. The first module
     *      is the entry module.
     *
     * @param globalScope
     *      The global scope to be filled with the module definitions. May contain app-defined
     *      symbols like types.
     *
     * @return A map that associates AST nodes to scopes to be used when traversing ASTs.
     */
    public static Map<ParserRuleContext, Scope> extractSymbols(Collection<ParsedModule> modules, GlobalScope globalScope) {
        final Map<ParserRuleContext, Scope> scopes = new HashMap<>();
        for (final ParsedModule module : modules) {
            final TypeWalker typeWalker = new TypeWalker(module.moduleName(), globalScope, scopes);
            module.ast().accept(typeWalker);
        }
        for (final ParsedModule module : modules) {
            final SymbolWalker symbolWalker = new SymbolWalker(globalScope, scopes);
            module.ast().accept(symbolWalker);
        }
        return scopes;
    }
}
