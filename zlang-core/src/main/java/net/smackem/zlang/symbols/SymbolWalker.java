package net.smackem.zlang.symbols;

import net.smackem.zlang.lang.ZLangBaseVisitor;
import net.smackem.zlang.lang.ZLangParser;

public class SymbolWalker extends ZLangBaseVisitor<Void> {
    @Override
    public Void visitProgram(ZLangParser.ProgramContext ctx) {
        return super.visitProgram(ctx);
    }
}
