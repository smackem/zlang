package net.smackem.zlang.symbols;

public final class Scopes {
    private Scopes() { }

    public static <T extends Symbol> T enclosingSymbol(Scope scope, Class<T> clazz) {
        while (true) {
            if (scope == null) {
                return null;
            }
            if (clazz.isAssignableFrom(scope.getClass())) {
                return clazz.cast(scope);
            }
            scope = scope.enclosingScope();
        }
    }
}
