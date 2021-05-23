package net.smackem.zlang.symbols;

import org.antlr.v4.runtime.ParserRuleContext;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public record ProgramStructure(int globalSegmentSize, GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
    public Collection<VariableSymbol> globals() {
        return globalScope.symbols().stream()
                .filter(s -> s instanceof ModuleSymbol)
                .flatMap(s -> ((ModuleSymbol) s).symbols().stream())
                .filter(s -> s instanceof VariableSymbol)
                .map(s -> (VariableSymbol) s)
                .collect(Collectors.toList());
    }

    public Collection<Type> emittedTypes() {
        return globalScope.symbols().stream()
                .filter(s -> s instanceof AggregateTypeSymbol && !(s instanceof ArrayType))
                .map(s -> (Type) s)
                .toList();
    }
}
