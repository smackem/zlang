package net.smackem.zlang.modules;

import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.lang.ZLangParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.stream.Collectors;

class DependencyWalker extends ZLangBaseVisitor<Void> {
    private String declaredModuleName;
    private List<String> dependencies;

    String declaredModuleName() {
        return this.declaredModuleName;
    }

    List<String> dependencies() {
        return this.dependencies != null
                ? this.dependencies
                : List.of();
    }

    @Override
    public Void visitModuleDecl(ZLangParser.ModuleDeclContext ctx) {
        this.declaredModuleName = ctx.Ident().getText();
        return super.visitModuleDecl(ctx);
    }

    @Override
    public Void visitUsesClause(ZLangParser.UsesClauseContext ctx) {
        this.dependencies = ctx.Ident().stream()
                .map(ParseTree::getText)
                .collect(Collectors.toList());
        return null;
    }

    @Override
    public Void visitGlobalDecl(ZLangParser.GlobalDeclContext ctx) {
        // no need to walk any other declaration
        return null;
    }
}
