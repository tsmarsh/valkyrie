package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.Optional;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.IO.file;
import static com.tailoredshapes.underbar.IO.resource;
import static com.tailoredshapes.valkyrie.util.Response.StatusCode.*;
import static org.junit.Assert.*;
import static com.tailoredshapes.valkyrie.util.Response.*;

/**
 * Created by tmarsh on 10/25/16.
 */
public class ResponseTest {

    String root = resource("/").getPath();

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
    public void mightFindAFile() throws Exception {
        assertTrue(findFileNamed(new File(root + "/lib"), "index.html").isPresent());
        assertFalse(findFileNamed(new File(root + "/bil"), "index.html").isPresent());
    }

    @Test
    public void canSearchForAFile() throws Exception {
        assertTrue(findFileStartingWith(new File(root + "/lib"), "index").isPresent());
        assertFalse(findFileStartingWith(new File(root + "/bil"), "index.html").isPresent());
    }

    @Test
    public void canSearchForAnIndexFile() throws Exception {
        assertTrue(findIndexFile(new File(root + "/lib")).isPresent());
        assertTrue(findIndexFile(new File(root + "/lib2")).isPresent());
        assertTrue(findIndexFile(new File(root + "/lib3")).isPresent());
        assertFalse(findIndexFile(new File(root + "/bil")).isPresent());
    }

    @Test
    public void canSafelyFindFile() throws Exception {
        File expected = file(resource("/lib/index.html"));
        File actual = safelyFindFile("lib/index.html", stash("root", this.root));
        assertEquals(expected, actual);
    }

    @Test
    public void canFindIndexFile() throws Exception {
        File expected = file(resource("/lib/index.html"));
        File actual = findFile("lib", stash("root", this.root, "indexFiles?", true));
        assertEquals(expected, actual);
        actual = findFile("lib/index.html", stash("root", this.root));
        assertEquals(expected, actual);
        assertNull(findFile("bil", stash("root", this.root)));
    }

    @Test
    public void canGenerateFileDate() throws Exception {
        File file = file(resource("/lib/index.html"));

        Stash expected = stash("content", file, "content-length", 3, "last-modified", new Date(file.lastModified()))
        Stash actual = fileData(file);
        assertEquals(expected, actual);
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