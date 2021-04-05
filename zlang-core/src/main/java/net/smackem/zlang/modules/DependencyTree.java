package net.smackem.zlang.modules;

import net.smackem.zlang.lang.SyntaxTree;
import net.smackem.zlang.lang.CompilationErrorException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DependencyTree {
    private final String moduleName;
    private final SyntaxTree ast;
    private final List<DependencyTree> dependencies = new ArrayList<>();

    private DependencyTree(String moduleName, SyntaxTree ast) {
        this.moduleName = moduleName;
        this.ast = ast;
    }

    public String moduleName() {
        return this.moduleName;
    }

    public SyntaxTree ast() {
        return this.ast;
    }

    public List<DependencyTree> dependencies() {
        return this.dependencies;
    }

    /**
     * @return a collection of all items below this {@link DependencyTree}, including
     *      {@code this} and all transient dependencies - in breadth-first order, beginning
     *      with {@code this} {@link DependencyTree}.
     */
    public Collection<DependencyTree> flatten() {
        final List<DependencyTree> list = new ArrayList<>();
        final Deque<DependencyTree> queue = new LinkedList<>();
        queue.add(this);
        while (queue.isEmpty() == false) {
            final DependencyTree item = queue.pollFirst();
            list.add(item);
            queue.addAll(item.dependencies);
        }
        return list;
    }

    public static DependencyTree parse(String moduleName, SourceFileLocation location)
            throws IOException, CompilationErrorException {
        final Set<String> visited = new HashSet<>();
        return parseRecurse(moduleName, location, visited);
    }

    private static DependencyTree parseRecurse(String moduleName, SourceFileLocation location, Set<String> visited)
            throws IOException, CompilationErrorException {
        final InputStream is = location.openSource(moduleName);
        if (is == null) {
            throw new FileNotFoundException("module '" + moduleName + "' not found");
        }

        final SyntaxTree ast = SyntaxTree.parse(moduleName, is);
        final DependencyTree tree = new DependencyTree(moduleName, ast);
        final DependencyWalker walker = new DependencyWalker();
        ast.accept(walker);

        if (Objects.equals(walker.declaredModuleName(), moduleName) == false) {
            throw new CompilationErrorException("module name '%s' does not match file name '%s'"
                    .formatted(walker.declaredModuleName(), moduleName));
        }

        for (final String dependency : walker.dependencies()) {
            if (visited.add(dependency)) {
                tree.dependencies.add(parse(dependency, location));
            }
        }
        return tree;
    }
}
