package net.smackem.zlang.modules;

import net.smackem.zlang.lang.CompilationErrorException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class ParsedModules {
    private ParsedModules() { }

    public static List<ParsedModule> single(String source) throws IOException, CompilationErrorException {
        return List.of(ParsedModule.parse("entry",
                SourceFileLocations.ofMap(Map.of("entry", source))));
    }
}
