package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.valkyrie.middleware.ContentType.contentTypeResponse;
import static org.junit.Assert.*;


public class ContentTypeTest {
    @Test
    public void shouldDoNothingIfContentTypePresent() throws Exception {
        Stash res = stash("headers", stash("Content-Type", "foo/bar"));
        assertEquals(res, contentTypeResponse(res, null));
    }

    @Test
    public void shouldGuessAContentTypeBasedOnFileName() throws Exception {
        assertEquals(stash("headers", stash("Content-Type", "application/json")),
                     contentTypeResponse(stash("headers", stash()), stash("uri", "/foo.json")));
    }
}