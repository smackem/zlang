package net.smackem.zlang.symbols;

import com.google.common.base.Strings;
import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.modules.ParsedModule;
import net.smackem.zlang.modules.SourceFileLocation;
import net.smackem.zlang.modules.SourceFileLocations;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.RuleNode;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SymbolExtractorTest {

    @Test
    public void testEmpty() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = parseModule("");
        final GlobalScope globalScope = new GlobalScope();
        final Map<ParserRuleContext, Scope> scopes = SymbolExtractor.extractSymbols(modules, globalScope);
        assertThat(scopes).hasSize(1);
        assertThat(scopes.values()).extracting(Scope::scopeName).contains("main");
        assertThat(scopes.values()).allMatch(scope -> scope instanceof ModuleSymbol);
        assertThat(globalScope.symbols()).hasSize(BuiltInTypeSymbol.builtInTypes().size() + 1);
    }

    @Test
    public void testSingleType() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = parseModule("""
                struct StructType {
                    field: int
                }
                """);
        final GlobalScope globalScope = new GlobalScope();
        final Map<ParserRuleContext, Scope> scopes = SymbolExtractor.extractSymbols(modules, globalScope);
        assertThat(scopes).isNotEmpty();
        assertThat(scopes).hasSize(2);
        assertThat(scopes.values()).extracting(Scope::scopeName).contains("main", "StructType");
        System.out.println(symbolText(modules, globalScope, scopes));
    }

    @Test
    public void testComplexModule() throws IOException, CompilationErrorException {
        final List<ParsedModule> modules = parseModule("""
                struct StructType {
                    field: int
                }
                fn function(i: int) -> int {
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
        final Map<ParserRuleContext, Scope> scopes = SymbolExtractor.extractSymbols(modules, globalScope);
        final String symText = symbolText(modules, null, scopes);
        System.out.println(symText);
        assertThat(symText).isEqualTo("""
                > main: ModuleSymbol
                    - StructType: StructSymbol{null}
                    - UnionType: UnionSymbol{null}
                    - function: FunctionSymbol{int}
                    > StructType: StructSymbol
                        - field: FieldSymbol{int}
                        - method: MethodSymbol{byte}
                    > function: FunctionSymbol
                        - i: ConstantSymbol{int}
                        > null: BlockScope
                            - o: ConstantSymbol{object}
                            > null: BlockScope
                                - v: ConstantSymbol{float}
                    > method: MethodSymbol
                        - self: ConstantSymbol{StructType}
                        > null: BlockScope
                    > UnionType: UnionSymbol
                        - integer: FieldSymbol{int}
                        - real: FieldSymbol{float}
                        - str: FieldSymbol{string}
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
        final Map<ParserRuleContext, Scope> scopes = SymbolExtractor.extractSymbols(modules, globalScope);
        final String symText = symbolText(modules, globalScope, scopes);
        System.out.println(symText);
        assertThat(symText).isEqualTo("""
                > null: GlobalScope
                    - int: BuiltInTypeSymbol{null}
                    - float: BuiltInTypeSymbol{null}
                    - byte: BuiltInTypeSymbol{null}
                    - object: BuiltInTypeSymbol{null}
                    - runtime_ptr: BuiltInTypeSymbol{null}
                    - string: BuiltInTypeSymbol{null}
                    - bool: BuiltInTypeSymbol{null}
                    - main: ModuleSymbol{null}
                    - dep: ModuleSymbol{null}
                    > main: ModuleSymbol
                        - File: StructSymbol{null}
                        - main: FunctionSymbol{null}
                        > File: StructSymbol
                            - handle: FieldSymbol{runtime_ptr}
                            - name: FieldSymbol{string}
                        > main: FunctionSymbol
                            > null: BlockScope
                                - f: ConstantSymbol{File}
                                - x: VariableSymbol{int}
                    > dep: ModuleSymbol
                        - Number: UnionSymbol{null}
                        > Number: UnionSymbol
                            - integer: FieldSymbol{int}
                            - real: FieldSymbol{float}
                            - unsigned: FieldSymbol{byte}
                """);
    }

    private static List<ParsedModule> parseModule(String source) throws IOException, CompilationErrorException {
        return List.of(ParsedModule.parse("main",
                SourceFileLocations.ofMap(Map.of("main", source))));
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
                buffer.append("    - %s: %s{%s}\n".formatted(
                        symbol.name(), symbol.getClass().getSimpleName(), symbol.type() != null ? symbol.type().typeName() : null));
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