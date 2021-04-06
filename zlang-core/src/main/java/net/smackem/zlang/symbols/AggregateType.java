package net.smackem.zlang.symbols;

import java.util.Collection;

public interface AggregateType extends Type {
    Collection<Type> implementedInterfaces();
}
