package net.smackem.zlang.modules;

import net.smackem.zlang.lang.CompilationErrorException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ParsedModuleTest {
    @Test
    public void testNoDependencies() throws IOException, CompilationErrorException {
        final SourceFileLocation sources = SourceFileLocations.ofMap(Map.of(
                "main", "module main\n"));
        final ParsedModule pm = ParsedModule.parse("main", sources);
        assertThat(pm.moduleName()).isEqualTo("main");
        assertThat(pm.dependencies()).isEmpty();
        assertThat(pm.ast()).isNotNull();
    }

    @Test
    public void testSingleDependency() throws IOException, CompilationErrorException {
        final SourceFileLocation sources = SourceFileLocations.ofMap(Map.of(
                "main", "module main uses dep\n",
                "dep", "module dep\n"));
        final ParsedModule pm = ParsedModule.parse("main", sources);
        assertThat(pm.moduleName()).isEqualTo("main");
        assertThat(pm.dependencies()).hasSize(1);
        final ParsedModule dep = pm.dependencies().get(0);
        assertThat(dep.moduleName()).isEqualTo("dep");
        assertThat(dep.dependencies()).isEmpty();
        assertThat(pm.flatten()).extracting(ParsedModule::moduleName)
                .containsExactly("main", "dep");
    }

    @Test
    public void testMultiDependencies() throws IOException, CompilationErrorException {
        final SourceFileLocation sources = SourceFileLocations.ofMap(Map.of(
                "main", "module main uses dep1, dep2\n",
                "dep1", "module dep1\n",
                "dep2", "module dep2\n"));
        final ParsedModule pm = ParsedModule.parse("main", sources);
        assertThat(pm.moduleName()).isEqualTo("main");
        assertThat(pm.dependencies()).extracting(ParsedModule::moduleName)
                .containsExactly("dep1", "dep2");
        assertThat(pm.dependencies()).extracting(ParsedModule::dependencies)
                .allMatch(List::isEmpty);
        assertThat(pm.flatten()).extracting(ParsedModule::moduleName)
                .containsExactly("main", "dep1", "dep2");
    }

    @Test
    public void testTransientDependencies() throws IOException, CompilationErrorException {
        final SourceFileLocation sources = SourceFileLocations.ofMap(Map.of(
                "main", "module main uses dep1, dep2\n",
                "dep1", "module dep1 uses dep11, dep12\n",
                "dep2", "module dep2\n",
                "dep11", "module dep11 uses dep111\n",
                "dep12", "module dep12 uses dep121\n",
                "dep111", "module dep111\n",
                "dep121", "module dep121\n"));
        final ParsedModule pm = ParsedModule.parse("main", sources);
        assertThat(pm.moduleName()).isEqualTo("main");
        assertThat(pm.dependencies()).extracting(ParsedModule::moduleName)
                .containsExactly("dep1", "dep2");
        assertThat(pm.flatten()).extracting(ParsedModule::moduleName)
                .containsExactlyInAnyOrder("main", "dep1", "dep2", "dep11", "dep12", "dep111", "dep121");
    }
}