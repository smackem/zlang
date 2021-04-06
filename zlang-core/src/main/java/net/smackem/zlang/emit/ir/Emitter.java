package net.smackem.zlang.emit.ir;

import net.smackem.zlang.symbols.GlobalScope;
import net.smackem.zlang.symbols.Scope;
import net.smackem.zlang.symbols.ScopeWalker;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;

public class Emitter extends ScopeWalker {
    protected Emitter(GlobalScope globalScope, Map<ParserRuleContext, Scope> scopes) {
        super(globalScope, scopes);
    }
}
