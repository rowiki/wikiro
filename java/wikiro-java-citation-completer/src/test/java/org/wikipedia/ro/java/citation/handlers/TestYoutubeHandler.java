package org.wikipedia.ro.java.citation.handlers;

import java.util.Optional;

import org.junit.Assert;

public class TestYoutubeHandler
{

    public void testYoutubeTitle()
    {
        YoutubeHandler sut = new YoutubeHandler();
        Optional<String> citation = sut.processCitationParams("https://www.youtube.com/watch?v=365LJqPXx40");
        
        Assert.assertTrue("Citation should be present", citation.isPresent());
    }
}
