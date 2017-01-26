package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.IO.*;
import static com.tailoredshapes.underbar.UnderBar.optional;
import static com.tailoredshapes.valkyrie.util.Request.*;
import static org.junit.Assert.*;

/**
 * Created by tmarsh on 12/19/16.
 */
public class RequestTest {


    @Test
    public void canExtractContentType() throws Exception {
        Stash request = stash("headers", stash("content-type", "text/html"));
        assertEquals(optional("text/html"), contentType(request));
    }

    @Test
    public void behavesWhenThereIsNoContentType() throws Exception {
        Stash request = stash("headers", stash());
        assertEquals(optional(), contentType(request));
    }

    @Test
    public void canDetectIfRequestIsAForm() throws Exception {
        Stash request = stash("headers", stash("content-type", "application/x-www-form-urlencoded"));
        assertTrue(isURLEncodedForm(request));
    }

    @Test
    public void canDetectIfRequestIsNotAForm() throws Exception {
        Stash request = stash("headers", stash());
        assertFalse(isURLEncodedForm(request));
    }

    @Test
    public void canExtractCharacterEncoding() throws Exception {
        Stash request = stash("headers", stash("content-type", "application/json; charset=utf-8"));
        assertEquals(optional("utf-8"), characterEncoding(request));
    }

    @Test
    public void canExtractStringFromBody() throws Exception {
        assertEquals(optional(), bodyString(stash("body", null)));
        assertEquals(optional("Hello, World!"), bodyString(stash("body", "Hello, World!")));
        assertEquals(optional("Hello, World!"), bodyString(stash("body", file(resource("/test.txt")))));
        assertEquals(optional("Hello, World!"), bodyString(stash("body", stringInputStream("Hello, World!"))));
    }

    @Test
    public void canExtractPathInfo() throws Exception {
        assertEquals(optional(), pathInfo(stash()));
        assertEquals(optional("path-info"), pathInfo(stash("path-info", "path-info")));
        assertEquals(optional("path-info"), pathInfo(stash("uri", "path-info")));
    }

    @Test
    public void canDetectAContext() throws Exception {
        assertTrue(hasContext(stash("uri", "contextpath"), "context"));
        assertFalse(hasContext(stash("uri", "contextpath"), "textcon"));
        assertFalse(hasContext(stash(), "textcon"));
    }

    @Test
    public void shouldSetAContext() throws Exception {
        assertEquals(
                stash("uri", "contextpathinfo", "path-info", "pathinfo", "context", "context"),
                setContext(stash("uri", "contextpathinfo"), "context"));
    }

    @Test
    public void canFormAUrlFromARequest() throws Exception {
        Stash request = stash("scheme", "http", "headers", stash("host", "localhost"), "uri", "/hello");
        assertEquals("http://localhost/hello", requestURL(request));
    }

    @Test
    public void canFormAUrlFromARequestWithQueryString() throws Exception {
        Stash request = stash("scheme", "http", "headers", stash("host", "localhost"), "uri", "/hello", "query-string", "foo=5");
        assertEquals("http://localhost/hello?foo=5", requestURL(request));

    }
}