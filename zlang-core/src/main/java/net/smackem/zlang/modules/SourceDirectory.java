package net.smackem.zlang.modules;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class SourceDirectory implements SourceFileLocation {
    private final Path path;
    private final String defaultFileExtension;

    /**
     * Initializes a new instance of {@link SourceDirectory}, which provides
     * source files from a file system directory.
     *
     * @param path
     *      The path to the source directory.
     *
     * @param defaultFileExtension
     *      The default extension to append to the passed module names. If it does
     *      not begin with a {@code '.'}, the dot is prepended.
     */
    public SourceDirectory(Path path, String defaultFileExtension) {
        this.path = path;
        Objects.requireNonNull(defaultFileExtension);
        if (defaultFileExtension.startsWith(".") == false) {
            defaultFileExtension = "." + defaultFileExtension;
        }
        this.defaultFileExtension = defaultFileExtension;
    }

    @Override
    public InputStream openSource(String moduleName) throws IOException {
        if (moduleName.contains(".") == false) {
            moduleName = moduleName + this.defaultFileExtension;
        }
        final Path filePath = Path.of(this.path.toString(), moduleName);
        return Files.newInputStream(filePath, StandardOpenOption.READ);
    }
}
