package net.smackem.zlang.modules;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SourceFileLocations {
    private SourceFileLocations() { }

    public static SourceFileLocation ofMap(Map<String, String> moduleSources) {
        return moduleName -> {
            final String source = moduleSources.get(moduleName);
            return source != null
                    ? new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))
                    : null;
        };
    }
}
