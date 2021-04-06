package net.smackem.zlang.modules;

import net.smackem.zlang.lang.SyntaxTree;
import net.smackem.zlang.lang.CompilationErrorException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ParsedModule {
    private final String moduleName;
    private final SyntaxTree ast;
    private final List<ParsedModule> dependencies = new ArrayList<>();

    private ParsedModule(String moduleName, SyntaxTree ast) {
        this.moduleName = moduleName;
        this.ast = ast;
    }

    public String moduleName() {
        return this.moduleName;
    }

    public SyntaxTree ast() {
        return this.ast;
    }

    public List<ParsedModule> dependencies() {
        return this.dependencies;
    }

    /**
     * @return a collection of all items below this {@link ParsedModule}, including
     *      {@code this} and all transient dependencies - in breadth-first order, beginning
     *      with the furthest transient dependency.
     */
    public Collection<ParsedModule> flatten() {
        final List<ParsedModule> list = new ArrayList<>();
        final Deque<ParsedModule> queue = new LinkedList<>();
        queue.offer(this);
        while (queue.isEmpty() == false) {
            final ParsedModule item = queue.pollFirst();
            list.add(item);
            queue.addAll(item.dependencies);
        }
        Collections.reverse(list);
        return list;
    }

    public static ParsedModule parse(String moduleName, SourceFileLocation location)
            throws IOException, CompilationErrorException {
        final Set<String> visited = new HashSet<>();
        return parseRecurse(moduleName, location, visited);
    }

    private static ParsedModule parseRecurse(String moduleName, SourceFileLocation location, Set<String> visited)
            throws IOException, CompilationErrorException {
        final InputStream is = location.openSource(moduleName);
        if (is == null) {
            throw new FileNotFoundException("module '" + moduleName + "' not found");
        }
        final SyntaxTree ast;
        try (is) {
            ast = SyntaxTree.parse(moduleName, is);
        }

        final ParsedModule tree = new ParsedModule(moduleName, ast);
        final DependencyWalker walker = new DependencyWalker();
        ast.accept(walker);

        if (walker.declaredModuleName() != null && walker.declaredModuleName().equals(moduleName) == false) {
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

    @Override
    public String toString() {
        return "ParsedModule{" +
               "moduleName='" + moduleName + '\'' +
               ", ast=" + ast +
               ", dependencies=" + dependencies +
               '}';
    }
}
