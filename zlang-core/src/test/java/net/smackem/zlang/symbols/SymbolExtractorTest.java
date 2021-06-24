package net.smackem.zlang.symbols;

import com.google.common.base.Strings;
import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.ParsedModules;
import net.smackem.zlang.modules.SourceFileLocation;
import net.smackem.zlang.modules.SourceFileLocations;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.RuleNode;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SymbolExtractorTest {

    @Test
    public void testEmpty() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = ParsedModules.single("");
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        assertThat(ps.scopes()).hasSize(1);
        assertThat(ps.scopes().values()).extracting(Scope::scopeName).contains("main");
        assertThat(ps.scopes().values()).allMatch(scope -> scope instanceof ModuleSymbol);
        // one error is expected: no main method
        assertThat(errors).hasSize(1);
        assertThat(errors).allMatch(s -> s.contains("main"));
    }

    @Test
    public void testSingleType() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct StructType {
                    field: int
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        assertThat(ps.scopes()).isNotEmpty();
        assertThat(ps.scopes()).hasSize(2);
        assertThat(ps.scopes().values()).extracting(Scope::scopeName).contains("main", "StructType");
        System.out.println(symbolText(modules, globalScope, ps.scopes()));
        // one error is expected: no main method
        assertThat(errors).hasSize(1);
        assertThat(errors).allMatch(s -> s.contains("main"));
    }

    @Test
    public void testComplexModule() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct StructType {
                    field: int
                }
                fn main(i: int) -> int {
                    let o: object = nil
                    if i > 0 {
                        let v: float = 1.0
                    }
                }
                fn StructType::method() -> byte {
                }
                union UnionType {
                    integer: int
                    real: float
                    str: string
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final String symText = symbolText(modules, null, ps.scopes());
        System.out.println(symText);
        assertThat(symText).isEqualTo("""
                > main: ModuleSymbol
                    - StructType: StructSymbol{null}@0
                    - UnionType: UnionSymbol{null}@0
                    - main: FunctionSymbol{int}@0
                    > StructType: StructSymbol
                        - field: FieldSymbol{int}@0
                        - method: MethodSymbol{byte}@0
                    > main: FunctionSymbol
                        - i: ConstantSymbol{int}@1
                        > null: BlockScope
                            - o: ConstantSymbol{object}@2
                            > null: BlockScope
                                - v: ConstantSymbol{float}@3
                    > method: MethodSymbol
                        - self: SelfSymbol{StructType}@1
                        > null: BlockScope
                    > UnionType: UnionSymbol
                        - @flag: FieldSymbol{byte}@0
                        - integer: FieldSymbol{int}@1
                        - real: FieldSymbol{float}@1
                        - str: FieldSymbol{string}@1
                """);
        // one error is expected: main method signature does not fit
        assertThat(errors).hasSize(1);
        assertThat(errors).allMatch(s -> s.contains("main"));
    }

    @Test
    public void testModuleWithInterface() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = ParsedModules.single("""
                interface Readable {
                    fn read(buf: byte[]) -> int
                }
                struct Socket {
                    address: string
                } is Readable
                fn Socket::read(buf: byte[]) -> int {
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final String symText = symbolText(modules, null, ps.scopes());
        System.out.println(symText);
        assertThat(symText).isEqualTo("""
                > main: ModuleSymbol
                    - Readable: InterfaceSymbol{null}@0
                    - Socket: StructSymbol{null}@0
                    > Readable: InterfaceSymbol
                        - read: InterfaceMethodSymbol{int}@0
                        > read: InterfaceMethodSymbol
                            - self: SelfSymbol{Readable}@1
                            - buf: ConstantSymbol{Array<byte>}@0
                    > Socket: StructSymbol
                        - address: FieldSymbol{string}@0
                        - read: MethodSymbol{int}@0
                    > read: MethodSymbol
                        - self: SelfSymbol{Socket}@1
                        - buf: ConstantSymbol{Array<byte>}@2
                        > null: BlockScope
                """);
    }

    @Test
    public void testModuleWithInterfaceTwistedOrder() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = ParsedModules.single("""
                struct Socket {
                    address: string
                } is Readable
                fn Socket::read(buf: byte[]) -> int {
                }
                interface Readable {
                    fn read(buf: byte[]) -> int
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final String symText = symbolText(modules, null, ps.scopes());
        System.out.println(symText);
    }

    @Test
    public void testGlobals() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = ParsedModules.single("""
                let a: int = 0
                let b: float = 0
                let c: byte = 0
                let d: runtime_ptr = null_ptr
                fn main() {
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final String symText = symbolText(modules, null, ps.scopes());
        System.out.println(symText);
    }

    @Test
    public void testLocals() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = ParsedModules.single("""
                fn noParams() {
                    var a: int = 0
                    var b: float = 0.0
                }
                fn twoParams(p1: int, p2: bool) {
                    var a: int = 0
                    var b: float = 0.0
                }
                fn twoParamsNested(p1: int, p2: bool) {
                    var a: int = 0
                    var b: float = 0.0
                    if false {
                        let c: byte = 0
                        for d: int in 0 .. 10 {
                        }
                    }
                }
                struct X {
                    field: int
                }
                fn X::oneParamNestedMethod(p1: byte) {
                    var a: int = 0
                    var b: float = 0.0
                    for c: byte in new byte[100] {
                        let d: byte = c
                    }
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final String symText = symbolText(modules, null, ps.scopes());
        System.out.println(symText);
        assertThat(symText).isEqualTo("""
                > main: ModuleSymbol
                    - X: StructSymbol{null}@0
                    - noParams: FunctionSymbol{null}@0
                    - twoParams: FunctionSymbol{null}@0
                    - twoParamsNested: FunctionSymbol{null}@0
                    > noParams: FunctionSymbol
                        > null: BlockScope
                            - a: VariableSymbol{int}@1
                            - b: VariableSymbol{float}@2
                    > twoParams: FunctionSymbol
                        - p1: ConstantSymbol{int}@1
                        - p2: ConstantSymbol{bool}@2
                        > null: BlockScope
                            - a: VariableSymbol{int}@3
                            - b: VariableSymbol{float}@4
                    > twoParamsNested: FunctionSymbol
                        - p1: ConstantSymbol{int}@1
                        - p2: ConstantSymbol{bool}@2
                        > null: BlockScope
                            - a: VariableSymbol{int}@3
                            - b: VariableSymbol{float}@4
                            > null: BlockScope
                                - c: ConstantSymbol{byte}@5
                                > null: BlockScope
                                    - d: ConstantSymbol{int}@6
                                    > null: BlockScope
                    > X: StructSymbol
                        - field: FieldSymbol{int}@0
                        - oneParamNestedMethod: MethodSymbol{null}@0
                    > oneParamNestedMethod: MethodSymbol
                        - self: SelfSymbol{X}@1
                        - p1: ConstantSymbol{byte}@2
                        > null: BlockScope
                            - a: VariableSymbol{int}@3
                            - b: VariableSymbol{float}@4
                            > null: BlockScope
                                - c: ConstantSymbol{byte}@5
                                > null: BlockScope
                                    - d: ConstantSymbol{byte}@6
                """);
    }

    @Test
    public void testMultiModule() throws IOException, CompilationErrorException {
        final String mainSource = """
                module main uses dep
                struct File {
                    handle: runtime_ptr
                    name: string
                }
                fn main() {
                    let f: File = nil
                    var x: int = 0
                }
                """;
        final String depSource = """
                union Number {
                    integer: int
                    real: float
                    unsigned: byte
                }
                """;
        final SourceFileLocation loc = SourceFileLocations.ofMap(Map.of(
                "main", mainSource,
                "dep", depSource));
        final ParsedModule module = ParsedModule.parse("main", loc);
        final Collection<ParsedModule> modules = module.flatten();
        final GlobalScope globalScope = new GlobalScope();
        final Collection<String> errors = new ArrayList<>();
        final ProgramStructure ps = SymbolExtractor.extractSymbols(modules, globalScope, errors);
        final String symText = symbolText(modules, globalScope, ps.scopes());
        System.out.println(symText);
        assertThat(symText).isEqualTo("""
                > null: GlobalScope
                    - int: PrimitiveTypeSymbol{null}@0
                    - float: PrimitiveTypeSymbol{null}@0
                    - byte: PrimitiveTypeSymbol{null}@0
                    - object: PrimitiveTypeSymbol{null}@0
                    - runtime_ptr: PrimitiveTypeSymbol{null}@0
                    - string: StringType{null}@0
                    - bool: PrimitiveTypeSymbol{null}@0
                    - print: BuiltInFunctionSymbol{null}@20
                    - dep: ModuleSymbol{null}@0
                    - main: ModuleSymbol{null}@0
                    > dep: ModuleSymbol
                        - Number: UnionSymbol{null}@0
                        > Number: UnionSymbol
                            - @flag: FieldSymbol{byte}@0
                            - integer: FieldSymbol{int}@1
                            - real: FieldSymbol{float}@1
                            - unsigned: FieldSymbol{byte}@1
                    > main: ModuleSymbol
                        - File: StructSymbol{null}@0
                        - main: FunctionSymbol{null}@0
                        > File: StructSymbol
                            - handle: FieldSymbol{runtime_ptr}@0
                            - name: FieldSymbol{string}@8
                        > main: FunctionSymbol
                            > null: BlockScope
                                - f: ConstantSymbol{File}@1
                                - x: VariableSymbol{int}@2
                """);
        assertThat(errors).isEmpty();
    }

    private static String symbolText(Collection<ParsedModule> modules, GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        final StringBuilder buffer = new StringBuilder();
        final int indent;

        if (globalScope != null) {
            ScopeVisitor.printScope(globalScope, buffer, 0);
            indent = 1;
        } else {
            indent = 0;
        }

        for (final ParsedModule pm : modules) {
            final ScopeVisitor visitor = new ScopeVisitor(indent, scopes);
            pm.ast().accept(visitor);
            buffer.append(visitor.buffer);
        }
        return buffer.toString();
    }

    private static class ScopeVisitor extends ZLangBaseVisitor<Void> {
        final StringBuilder buffer = new StringBuilder();
        final Map<ParserRuleContext, Scope> moduleScopes;
        int indent;

        ScopeVisitor(int indent, Map<ParserRuleContext, Scope> moduleScopes) {
            this.indent = indent;
            this.moduleScopes = moduleScopes;
        }

        @Override
        public Void visitChildren(RuleNode node) {
            final Scope scope = this.moduleScopes.get((ParserRuleContext) node);
            if (scope != null) {
                enterScope(scope);
            }
            super.visitChildren(node);
            if (scope != null) {
                leaveScope();
            }
            return null;
        }

        static void printScope(Scope scope, StringBuilder buffer, int indent) {
            final String indentStr = Strings.repeat("    ", indent);
            buffer.append(indentStr);
            buffer.append("> %s: %s\n".formatted(
                    scope.scopeName(), scope.getClass().getSimpleName()));
            for (final Symbol symbol : scope.symbols()) {
                buffer.append(indentStr);
                buffer.append("    - %s: %s{%s}@%d\n".formatted(
                        symbol.name(), symbol.getClass().getSimpleName(), symbol.type() != null ? symbol.type().typeName() : null, symbol.address()));
            }
        }

        private void enterScope(Scope scope) {
            printScope(scope, this.buffer, this.indent);
            this.indent++;
        }

        private void leaveScope() {
            this.indent--;
        }
    }
}