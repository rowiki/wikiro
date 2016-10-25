package org.wikipedia.ro.astroe.generators;

import java.io.IOException;
import java.util.List;

public interface Generator {
    public List<String> getGeneratedTitles() throws IOException;
    public String getDescriptionKey();
    public int getNumberOfTextFields();
    public String[] getTextFieldsLabelKeys();
}
