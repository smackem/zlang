package net.smackem.zlang.symbols;

public class MethodSymbol extends FunctionSymbol {

    MethodSymbol(String name, Type type, MemberScope enclosingScope) {
        super(name, type, enclosingScope);
    }
}
