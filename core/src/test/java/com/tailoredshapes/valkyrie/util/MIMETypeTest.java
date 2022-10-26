package com.tailoredshapes.valkyrie.util;

import org.junit.Test;

import static com.tailoredshapes.underbar.ocho.UnderBar.hash;
import static com.tailoredshapes.valkyrie.util.MIMEType.extMimeType;
import static org.junit.Assert.assertEquals;

/**
 * Created by tmarsh on 2/9/17.
 */
public class MIMETypeTest {

    @Test
    public void shouldReturnAMimeTypeForAKnownType() throws Exception {
        assertEquals("application/json", extMimeType("foo.json"));
        assertEquals("text/plain", extMimeType("json"));
        assertEquals("text/plain", extMimeType("foo.undefined"));
    }

    @Test
    public void shouldReturnAMimeTypeForAnAdditionalType() throws Exception {
        assertEquals("application/json", extMimeType("foo.json", hash("undefined", "foo/bar")));
        assertEquals("foo/bar", extMimeType("foo.undefined", hash("undefined", "foo/bar")));
    }
}