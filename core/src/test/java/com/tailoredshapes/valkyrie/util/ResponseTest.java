package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import java.net.URL;
import java.util.Optional;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.IO.resource;
import static com.tailoredshapes.valkyrie.util.Response.StatusCode.*;
import static org.junit.Assert.*;
import static com.tailoredshapes.valkyrie.util.Response.*;

/**
 * Created by tmarsh on 10/25/16.
 */
public class ResponseTest {

    @Test
    public void canGenerateARedirect() throws Exception {
        Stash redirect = redirect("http://localhost/redirect");

        assertEquals(302, (int) redirect.get("status"));
        assertEquals("http://localhost/redirect", redirect.get("headers", Stash.class).get("Location"));
    }

    @Test
    public void canGenerateACreated() throws Exception {
        Stash created = created("http://localhost/created");

        assertEquals(201, (int) created.get("status"));
        assertEquals("http://localhost/created", created.get("headers", Stash.class).get("Location"));
    }

    @Test
    public void canGenerateACreatedWithBody() throws Exception {
        Stash created = created("http://localhost/created", "success");

        assertEquals(201, (int) created.get("status"));
        assertEquals("http://localhost/created", created.get("headers", Stash.class).get("Location"));
        assertEquals("success", created.get("body"));
    }

    @Test
    public void canGenerateAResponse() throws Exception {
        Stash response = response("success");

        assertEquals(200, (int) response.get("status"));
        assertEquals("success", response.get("body"));
    }

    @Test
    public void canEasilyChangeTheStatus() throws Exception {
        Stash response = response("success");

        status(response, MOVED_PERMANENTLY);
        assertEquals(301, (int) response.get("status"));
    }

    @Test
    public void canEasilyAddAHeader() throws Exception {
        Stash response = response("success");
        header(response, "Cookie", "foo=1");
        assertEquals("foo=1", response.get("headers", Stash.class).get("Cookie"));
    }

    @Test
    public void directoryTraversalTest() throws Exception {
        assertTrue(isDirectoryTraversal("../"));
        assertTrue(isDirectoryTraversal("\\.."));
        assertFalse(isDirectoryTraversal("/path/to/nowhere"));
    }

    @Test
    public void canParseACharacterSetOutOfAContentHeader() throws Exception {
        Stash response = stash("headers", stash("Content-Type", "text/plain; charset=UTF-16"));
        assertEquals("UTF-16", getCharset(response).get());
    }
}