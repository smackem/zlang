package net.smackem.zlang.symbols;

import java.util.stream.Collectors;

public class InterfaceMethodSymbol extends MethodSymbol {
    InterfaceMethodSymbol(String name, Type type, MemberScope enclosingScope) {
        super(name, type, enclosingScope);
    }

    @Override
    public String toString() {
        return "InterfaceMethodSymbol{" +
               "name=" + name() +
               ", returnType=" + type() +
               ", declaringType=" + enclosingScope() +
               ", parameters=" + symbols().stream()
                       .map(s -> s.name() + ':' + s.type().typeName())
                       .collect(Collectors.joining(", ")) +
               '}';
    }
}
