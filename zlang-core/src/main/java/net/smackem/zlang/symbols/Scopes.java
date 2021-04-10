package net.smackem.zlang.symbols;

public final class Scopes {
    private Scopes() { }

    public static <T extends Symbol> T enclosingSymbol(Scope scope, Class<T> clazz) {
        while (true) {
            if (scope == null) {
                return null;
            }
            if (scope.getClass().isAssignableFrom(clazz)) {
                return clazz.cast(scope);
            }
            scope = scope.enclosingScope();
        }
    }
}
