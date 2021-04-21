package net.smackem.zlang.symbols;

public class SelfSymbol extends ConstantSymbol {
    SelfSymbol(Type type) {
        super("self", type, false);
    }

    @Override
    public boolean isSelf() {
        return true;
    }
}
