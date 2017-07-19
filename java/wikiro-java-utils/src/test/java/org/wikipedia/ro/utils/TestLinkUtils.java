package org.wikipedia.ro.utils;

import org.junit.Assert;
import org.junit.Test;

public class TestLinkUtils {
    @Test
    public void testCreateLink() {
        
        Assert.assertEquals("[[gigibobo]]", LinkUtils.createLink("gigibobo", "gigibobo"));
        Assert.assertEquals("[[gigibobo]]", LinkUtils.createLink("gigibobo", null));
        Assert.assertEquals("[[Gigibobo|bobogigi]]", LinkUtils.createLink("gigibobo", "bobogigi"));
        Assert.assertEquals("bobogigi", LinkUtils.createLink(null, "bobogigi"));
        Assert.assertNull(LinkUtils.createLink(null, null));
        
    }
}
