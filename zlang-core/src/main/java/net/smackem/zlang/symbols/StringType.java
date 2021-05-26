package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

public class StringType extends AggregateTypeSymbol implements RegisterType {
    StringType() {
        super("string", null);
        defineBuiltInMethods();
    }

    @Override
    public RegisterTypeId id() {
        return RegisterTypeId.String;
    }

    @Override
    public int byteSize() {
        return 4;
    }

    private void defineBuiltInMethods() {
        try {
            defineBuiltInMethod(BuiltInType.INT.type(), BuiltInFunction.STRING_LENGTH);
        } catch (CompilationErrorException e) {
            throw new RuntimeException(e); // duplicate identifier -> programming error
        }
    }
}
