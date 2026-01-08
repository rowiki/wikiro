package org.wikipedia.ro.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class TestLinkUtils {
    @Test
    public void testCreateLink() {
        
        assertEquals("[[gigibobo]]", LinkUtils.createLink("gigibobo", "gigibobo"));
        assertEquals("[[gigibobo]]", LinkUtils.createLink("gigibobo", null));
        assertEquals("[[Gigibobo|bobogigi]]", LinkUtils.createLink("gigibobo", "bobogigi"));
        assertEquals("bobogigi", LinkUtils.createLink(null, "bobogigi"));
        assertNull(LinkUtils.createLink(null, null));
        
    }
}
