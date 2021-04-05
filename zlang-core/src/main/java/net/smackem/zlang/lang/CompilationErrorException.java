package net.smackem.zlang.lang;

import java.util.Collection;
import java.util.List;

public class CompilationErrorException extends Exception {
    private final List<String> messages;

    public CompilationErrorException(String message) {
        super(message);
        this.messages = List.of(message);
    }

    public CompilationErrorException(Collection<String> messages) {
        super(String.join(System.lineSeparator(), messages));
        this.messages = List.copyOf(messages);
    }

    public Collection<String> messages() {
        return this.messages;
    }
}
