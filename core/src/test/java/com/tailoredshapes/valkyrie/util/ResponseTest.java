package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import java.io.File;
import java.net.URLConnection;
import java.util.Date;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.io.IO.file;
import static com.tailoredshapes.underbar.ocho.UnderBar.optional;
import static com.tailoredshapes.valkyrie.util.Response.StatusCode.MOVED_PERMANENTLY;
import static com.tailoredshapes.valkyrie.util.Response.*;
import static com.tailoredshapes.valkyrie.util.Time.formatDate;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by tmarsh on 10/25/16.
 */
public class ResponseTest {

    String root = this.getClass().getResource("/").getPath();

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
        File expected = file(this.getClass().getResource("/lib/index.html"));
        File actual = safelyFindFile("lib/index.html", stash("root", this.root));
        assertEquals(expected, actual);
    }

    @Test
    public void canFindIndexFile() throws Exception {
        File expected = file(this.getClass().getResource("/lib/index.html"));
        File actual = findFile("lib", stash("root", this.root, "indexFiles?", true));
        assertEquals(expected, actual);
        actual = findFile("lib/index.html", stash("root", this.root));
        assertEquals(expected, actual);
        assertNull(findFile("bil", stash("root", this.root)));
    }

    @Test
    public void canGenerateFileDate() throws Exception {
        File file = file(this.getClass().getResource("/lib/index.html"));
        long date = file.lastModified() / 1000;
        Stash expected = stash("content", file, "content-length", 3, "last-modified", new Date(date * 1000));
        Stash actual = fileData(file);
        assertEquals(expected, actual);
    }

    @Test
    public void canGenerateAFileResponse() throws Exception {
        File file = file(this.getClass().getResource("/lib/index.html"));

        Stash expected = stash(
                "body", file,
                "headers", stash("Content-Length", 3, "Last-Modified", formatDate(new Date(file.lastModified()))),
                "status", 200);
        assertEquals(expected, fileResponse(root + "lib/index.html").get());
    }

    @Test
    public void canGenerateANotFoundResponse() throws Exception {
        Stash expected = stash(
                "body", null,
                "headers", stash(),
                "status", 404);
        assertEquals(expected, notFound(null));
    }

    @Test
    public void canSetTheContentLengthManually() throws Exception {
        Stash response = stash("headers", stash());
        contentLength(response, 5);
        assertEquals(stash("headers", stash("Content-Length", 5)), response);
    }

    @Test
    public void canSetTheLastModifiedManually() throws Exception {
        Stash response = stash("headers", stash());
        lastModified(response, new Date(613162785L));
        assertEquals(stash("headers", stash("Last-Modified", "Thu, 08 Jan 1970 02:19:22 UTC")), response);
    }

    @Test
    public void canSetTheContentTypeManually() throws Exception {
        Stash response = stash();
        contentType(response, "application/json");
        assertEquals(stash("headers", stash("Content-Type", "application/json")), response);
        charset(response, "UTF-16");
        assertEquals(stash("headers", stash("Content-Type", "application/json; charset=UTF-16")), response);
    }


    @Test
    public void canSetACookie() throws Exception {
        assertEquals(stash("cookies", stash("foo", stash("value", "bar"))), setCookie(stash(), "foo", "bar"));
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

    @Test
    public void canGenerateAResponseForAResource() throws Exception {
        File file = file(this.getClass().getResource("/lib/index.html"));
        Stash expected = stash("body", file, "headers", stash("Content-Length", 3, "Last-Modified", formatDate(new Date(file.lastModified()))), "status", 200);
        assertEquals(expected, resourceResponse("/lib/index.html", stash()).get());
    }

    @Test
    public void canDetectAResponse() throws Exception {
        assertFalse(isResponse(stash()));
        assertTrue(isResponse(stash("status", 200, "headers", stash())));
    }

    @Test
    public void canAddAnEndingSlash() throws Exception {
        assertEquals("foo/", addEndingSlash("foo"));
        assertEquals("foo/", addEndingSlash("foo/"));
    }

    @Test
    public void canGetContentLengthOfURLConnection() throws Exception {
        URLConnection mockConnection = mock(URLConnection.class);
        when(mockConnection.getContentLength()).thenReturn(3);
        assertEquals(3, (long) connectionContentLength(mockConnection).get());
        when(mockConnection.getContentLength()).thenReturn(-1);
        assertEquals(optional(), connectionContentLength(mockConnection));
    }

    @Test
    public void canGetContentLastModifiedOfURLConnection() throws Exception {
        URLConnection mockConnection = mock(URLConnection.class);
        when(mockConnection.getLastModified()).thenReturn(3L);
        assertEquals(3L, (long) connectionLastModified(mockConnection).get());
        when(mockConnection.getLastModified()).thenReturn(0L);
        assertEquals(optional(), Response.connectionLastModified(mockConnection));
    }
}