package net.smackem.zlang.symbols;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Map;

public record ProgramStructure(int globalSegmentSize, Map<ParserRuleContext, Scope> scopes) {
}
