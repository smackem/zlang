package net.smackem.zlang.symbols;

public interface MemberScope extends Scope {
    Symbol resolveMember(String name);
}
