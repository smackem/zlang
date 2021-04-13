package net.smackem.zlang.symbols;

import net.smackem.zlang.modules.ParsedModule;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.*;

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
     *      symbols like constants or types.
     *
     * @param outErrors
     *      A modifiable collection that receives error messages.
     *
     * @return The {@link ProgramStructure}.
     */
    public static ProgramStructure extractSymbols(Collection<ParsedModule> modules, GlobalScope globalScope, Collection<String> outErrors) {
        int globalSegmentSize = 0;
        final Map<ParserRuleContext, Scope> scopes = new HashMap<>();
        for (final ParsedModule module : modules) {
            final TypeWalker typeWalker = new TypeWalker(module.moduleName(), globalScope, scopes);
            module.ast().accept(typeWalker);
        }
        for (final ParsedModule module : modules) {
            final SymbolWalker symbolWalker = new SymbolWalker(globalScope, scopes, globalSegmentSize);
            module.ast().accept(symbolWalker);
            globalSegmentSize = symbolWalker.globalSegmentSize();
        }

        final long entryPointCount = scopes.values().stream()
                .flatMap(scope -> scope.symbols().stream())
                .filter(symbol -> symbol instanceof FunctionSymbol
                                  && ((FunctionSymbol) symbol).isEntryPoint())
                .count();
        if (entryPointCount == 0) {
            outErrors.add("the program does not define an entry point with the signature `fn main()`.");
        } else if (entryPointCount > 1) {
            outErrors.add("the program does defines multiple entry points with the signature `fn main()`.");
        }

        return new ProgramStructure(globalSegmentSize, globalScope, scopes);
    }
}
