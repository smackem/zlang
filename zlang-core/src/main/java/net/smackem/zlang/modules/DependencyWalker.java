package net.smackem.zlang.modules;

import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.lang.ZLangParser;

public class DependencyWalker extends ZLangBaseVisitor<Void> {
    @Override
    public Void visitModuleDecl(ZLangParser.ModuleDeclContext ctx) {
        return super.visitModuleDecl(ctx);
    }
}
