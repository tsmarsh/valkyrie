package com.tailoredshapes.valkyrie.middleware;

import org.junit.Test;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.valkyrie.middleware.Cookies.parseCookieHeader;
import static com.tailoredshapes.valkyrie.middleware.Cookies.parseCookies;
import static org.junit.Assert.assertEquals;

public class CookiesTest {
    @Test
    public void parseCookieHeaderTest() throws Exception {
        assertEquals(stash("foo_1", "2181b33d2ed3d8bfb292171d3055ad0c",
                "foo_lastvisit", "1486867396",
                "foo_lastactivity", "0"),
                parseCookieHeader("foo_1=2181b33d2ed3d8bfb292171d3055ad0c; foo_lastvisit=1486867396; foo_lastactivity=0"));
    }

    @Test
    public void shouldParseCookies() throws Exception {
        assertEquals(stash(), parseCookies(stash("headers", stash()), i -> i));
        assertEquals(stash("foo_1", "2181b33d2ed3d8bfb292171d3055ad0c"), parseCookies(stash("headers", stash("cookie", "foo_1=2181b33d2ed3d8bfb292171d3055ad0c")), i -> i));
    }
}