package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.Die.die;
import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.valkyrie.middleware.ContentType.contentTypeResponse;
import static com.tailoredshapes.valkyrie.middleware.ContentType.wrapContentType;
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

    @Test
    public void wrapsAHandlerAndInsertsTheContentType() throws Exception {
        Handler handler = wrapContentType((res) -> stash("headers", stash()));
        assertEquals(stash("headers", stash("Content-Type", "application/json")), handler.apply(stash("uri", "/foo.json")));
    }

    @Test
    public void wrapsAnAsyncHandlerAndInsertsTheContentType() throws Exception {
        AsyncHandler asyncHandler = wrapContentType((req, res, raise) -> res.apply(stash("headers", stash())));
        assertEquals(stash("headers", stash("Content-Type", "application/json")),
                asyncHandler.apply(
                        stash("uri", "/foo.json"),
                        (r) -> r,
                        (c) -> die(c, "Failed in test")));
    }


}