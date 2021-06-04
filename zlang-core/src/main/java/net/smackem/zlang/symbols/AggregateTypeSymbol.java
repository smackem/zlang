package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.CompilationErrorException;

import java.util.*;

public abstract class AggregateTypeSymbol extends Symbol implements AggregateType, MemberScope {
    private final SymbolTable symbolTable;
    private final List<Type> implementedInterfaces = new ArrayList<>();
    private Map<InterfaceMethodSymbol, MethodSymbol> cachedVirtualTable;

    AggregateTypeSymbol(String name, Scope enclosingScope) {
        super(name, null);
        this.symbolTable = new SymbolTable(enclosingScope, name);
    }

    void addImplementedInterface(InterfaceSymbol symbol) {
        this.implementedInterfaces.add(Objects.requireNonNull(symbol));
    }

    String symbolTableString() {
        return this.symbolTable.toString();
    }

    @Override
    public String typeName() {
        return name();
    }

    @Override
    public Symbol resolveMember(String name) {
        final Symbol symbol = this.symbolTable.resolveMember(name);
        if (symbol != null) {
            return symbol;
        }
        for (final Type t : this.implementedInterfaces) {
            final MemberScope ms = (MemberScope) t;
            final Symbol ifcMember = ms.resolveMember(name);
            if (ifcMember != null) {
                return ifcMember;
            }
        }
        return null;
    }

    @Override
    public String scopeName() {
        return name();
    }

    @Override
    public void define(String name, Symbol symbol) throws CompilationErrorException {
        this.symbolTable.define(name, symbol);
    }

    @Override
    public Scope enclosingScope() {
        return this.symbolTable.enclosingScope();
    }

    @Override
    public Symbol resolve(String name) {
        return this.symbolTable.resolve(name);
    }

    @Override
    public Collection<Symbol> symbols() {
        return this.symbolTable.symbols();
    }

    @Override
    public Collection<Type> implementedInterfaces() {
        return Collections.unmodifiableCollection(this.implementedInterfaces);
    }

    @Override
    public RegisterType registerType() {
        return BuiltInType.OBJECT.type();
    }

    public Map<InterfaceMethodSymbol, MethodSymbol> buildVirtualTable() {
        if (this.cachedVirtualTable != null) {
            return this.cachedVirtualTable;
        }
        final Map<InterfaceMethodSymbol, MethodSymbol> map = new LinkedHashMap<>();
        for (final Type ifcType : this.implementedInterfaces) {
            final InterfaceSymbol ifc = (InterfaceSymbol) ifcType;
            for (final Symbol methodSymbol : ifc.symbols()) {
                if (methodSymbol instanceof InterfaceMethodSymbol == false) {
                    continue;
                }
                final InterfaceMethodSymbol ifcMethod = (InterfaceMethodSymbol) methodSymbol;
                final MethodSymbol implementingMethod = findMethodWithSignatureMatching(ifcMethod);
                if (implementingMethod == null) {
                    throw new RuntimeException(new CompilationErrorException(
                            "type '%s' does not implement method '%s' declared by '%s'".formatted(this, ifcMethod, ifc)));
                }
                map.put(ifcMethod, implementingMethod);
            }
        }
        this.cachedVirtualTable = map;
        return map;
    }

    private MethodSymbol findMethodWithSignatureMatching(FunctionSymbol function) {
        return symbols().stream()
                .filter(s -> s instanceof MethodSymbol)
                .map(s -> (MethodSymbol) s)
                .filter(method -> method.signatureMatches(function))
                .findFirst()
                .orElse(null);
    }

    void defineBuiltInMethod(Type returnType, BuiltInFunction bif, Type... parameterTypes) throws CompilationErrorException {
        final BuiltInMethodSymbol method = new BuiltInMethodSymbol(bif, returnType, this);
        int parameterIndex = 0;
        method.define(SelfSymbol.IDENT, new SelfSymbol(this));
        for (final Type parameterType : parameterTypes) {
            final String parameterName = "p" + parameterIndex++;
            method.define(parameterName, new ConstantSymbol(parameterName, parameterType, false));
        }
        this.symbolTable.define(bif.ident(), method);
    }

    void defineBuiltInFields(FieldSymbol... fields) throws CompilationErrorException {
        int address = 0;
        for (final FieldSymbol field : fields) {
            assert field.name().startsWith("@");
            assert field.declaringType() == this;
            define(field.name(), field);
            field.setAddress(address);
            address += field.type().registerType().byteSize();
        }
    }

    int sumFieldSizes() {
        int size = 0;
        for (final Symbol symbol : symbols()) {
            if (symbol instanceof FieldSymbol) {
                size += symbol.type().registerType().byteSize();
            }
        }
        return size;
    }
}
