package org.wikipedia.ro;

import java.io.IOException;
import java.util.List;

public interface Generator {
    public List<String> getGeneratedTitles() throws IOException;
}
