package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.ZLangParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;

class TypeWalker extends ScopeWalker<Void> {
    private final String fileName;

    TypeWalker(String fileName, GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        super(globalScope, scopes);
        this.fileName = fileName;
    }

    @Override
    public Void visitModule(ZLangParser.ModuleContext ctx) {
        final ModuleSymbol module = new ModuleSymbol(this.fileName, currentScope());
        defineSymbol(ctx, currentScope(), module);
        pushScope(ctx, module);
        super.visitModule(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitStructDecl(ZLangParser.StructDeclContext ctx) {
        final StructSymbol struct = new StructSymbol(ctx.Ident().getText(), currentScope());
        defineSymbol(ctx, currentScope(), struct);
        pushScope(ctx, struct);
        super.visitStructDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitUnionDecl(ZLangParser.UnionDeclContext ctx) {
        final UnionSymbol union = new UnionSymbol(ctx.Ident().getText(), currentScope());
        defineSymbol(ctx, currentScope(), union);
        pushScope(ctx, union);
        super.visitUnionDecl(ctx);
        popScope();
        return null;
    }

    @Override
    public Void visitInterfaceDecl(ZLangParser.InterfaceDeclContext ctx) {
        final InterfaceSymbol ifc = new InterfaceSymbol(ctx.Ident().getText(), currentScope());
        defineSymbol(ctx, currentScope(), ifc);
        pushScope(ctx, ifc);
        super.visitInterfaceDecl(ctx);
        popScope();
        return null;
    }
}
