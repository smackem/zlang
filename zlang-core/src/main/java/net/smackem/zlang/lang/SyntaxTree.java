package net.smackem.zlang.lang;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public class SyntaxTree {
    private final String fileName;
    private final ParseTree parseTree;

    private SyntaxTree(String fileName, ParseTree parseTree) {
        this.fileName = fileName;
        this.parseTree = parseTree;
    }

    public String fileName() {
        return this.fileName;
    }

    public <T> T accept(ZLangVisitor<T> visitor) {
        return this.parseTree.accept(visitor);
    }

    public static SyntaxTree parse(String fileName, InputStream is) throws IOException, CompilationErrorException {
        final List<String> errors = new ArrayList<>();
        final ParseTree tree = parse(fileName, is, errors);
        if (errors.isEmpty() == false) {
            throw new CompilationErrorException(errors);
        }
        return new SyntaxTree(fileName, tree);
    }

    private static ZLangParser.ModuleContext parse(String fileName, InputStream is, Collection<String> outErrors) throws IOException {
        final CharStream input = CharStreams.fromStream(is);
        final ZLangLexer lexer = new ZLangLexer(input);
        final ErrorListener errorListener = new ErrorListener(fileName);
        lexer.addErrorListener(errorListener);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final ZLangParser parser = new ZLangParser(tokens);
        parser.addErrorListener(errorListener);
        final ZLangParser.ModuleContext ast = parser.module();
        if (outErrors.addAll(errorListener.errors)) {
            return null;
        }
        return ast;
    }

    private static class ErrorListener implements ANTLRErrorListener {
        private final Collection<String> errors = new ArrayList<>();
        private final String fileName;

        ErrorListener(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String s, RecognitionException e) {
            this.errors.add(String.format("file %s line %d:%d: %s", this.fileName, line, pos, s));
        }

        @Override
        public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet) {
        }

        @Override
        public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet) {
        }

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {
        }
    }
}
