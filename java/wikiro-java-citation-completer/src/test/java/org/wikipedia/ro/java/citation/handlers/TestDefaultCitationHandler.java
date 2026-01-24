package org.wikipedia.ro.java.citation.handlers;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultCitationHandler
{
    @Test
    public void testCanProcessUrlWithUnderscoreInHostname() throws Exception
    {
        // URL with underscore in hostname is technically invalid per URI spec,
        // but the DefaultCitationHandler should handle it gracefully
        String urlWithUnderscore = "https://forza_azzurri.homestead.com/clubs_prof_j.html";
        
        DefaultCitationHandler handler = new DefaultCitationHandler();
        
        // Verify the handler can process the URL without throwing exceptions
        Optional<String> result = handler.processCitationParams(urlWithUnderscore);
        
        // The handler should be able to process it (may return empty if no citation found, but shouldn't throw)
        Assert.assertNotNull("Result should not be null", result);
    }
}
