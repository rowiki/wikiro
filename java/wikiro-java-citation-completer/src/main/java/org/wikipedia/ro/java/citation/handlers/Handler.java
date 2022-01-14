package org.wikipedia.ro.java.citation.handlers;

import java.util.Optional;

public interface Handler
{
    Optional<String> processCitationParams(String url);
}
