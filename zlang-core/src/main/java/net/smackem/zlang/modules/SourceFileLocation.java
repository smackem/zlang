package net.smackem.zlang.modules;

import java.io.IOException;
import java.io.InputStream;

public interface SourceFileLocation {
    InputStream openSource(String moduleName) throws IOException;
}
