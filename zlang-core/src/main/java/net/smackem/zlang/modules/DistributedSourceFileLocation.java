package net.smackem.zlang.modules;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class DistributedSourceFileLocation implements SourceFileLocation {
    private final List<SourceFileLocation> locations;

    public DistributedSourceFileLocation(Collection<SourceFileLocation> locations) {
        this.locations = List.copyOf(locations);
    }

    @Override
    public InputStream openSource(String moduleName) throws IOException {
        for (final SourceFileLocation location : this.locations) {
            final InputStream is = location.openSource(moduleName);
            if (is != null) {
                return is;
            }
        }
        return null;
    }
}
