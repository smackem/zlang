package net.smackem.zlang.symbols;

public class SelfSymbol extends ConstantSymbol {
    public static final String IDENT = "self";

    SelfSymbol(Type type) {
        super(IDENT, type, false);
    }

    @Override
    public boolean isSelf() {
        return true;
    }
}
