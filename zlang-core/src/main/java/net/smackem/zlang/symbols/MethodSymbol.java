package net.smackem.zlang.symbols;

import java.util.stream.Collectors;

public class MethodSymbol extends FunctionSymbol {

    MethodSymbol(String name, Type type, MemberScope enclosingScope) {
        super(name, type, enclosingScope);
    }

    @Override
    public String toString() {
        return "MethodSymbol{" +
               "name=" + name() +
               "returnType=" + type() +
               "declaringType=" + enclosingScope() +
               "parameters=" + parameters().stream()
                       .map(s -> s.name() + ':' + s.type().typeName())
                       .collect(Collectors.joining(", ")) +
               '}';
    }
}
