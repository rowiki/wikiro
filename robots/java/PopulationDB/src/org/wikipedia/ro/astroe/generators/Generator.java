package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.List;

public interface Generator {
    public List<String> getGeneratedTitles() throws IOException;
}
