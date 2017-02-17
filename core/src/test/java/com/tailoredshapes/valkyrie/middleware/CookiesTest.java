package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.underbar.Dates;
import com.tailoredshapes.valkyrie.core.AsyncHandler;
import com.tailoredshapes.valkyrie.core.Handler;
import org.junit.Test;

import java.util.Date;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.Die.die;
import static com.tailoredshapes.valkyrie.middleware.Cookies.*;
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
        assertEquals(stash(), parseCookies(stash("headers", stash()), (i, x) -> i));
        assertEquals(stash("foo_1", "2181b33d2ed3d8bfb292171d3055ad0c"),
                parseCookies(stash("headers", stash("cookie", "foo_1=2181b33d2ed3d8bfb292171d3055ad0c")),
                        (i, x) -> i));
    }

    @Test
    public void shouldParseOutTheCookies() throws Exception {
        assertEquals(stash(
                "headers", stash("cookie", "foo_1=2181b33d2ed3d8bfb292171d3055ad0c"),
                "cookies", stash("foo_1", "2181b33d2ed3d8bfb292171d3055ad0c")),
                cookiesRequest(stash("headers", stash("cookie", "foo_1=2181b33d2ed3d8bfb292171d3055ad0c"))));
    }

    @Test
    public void shouldDoNothingIfCookiesAlreadyExist() throws Exception {
        assertEquals(stash("cookies", stash("foo_1", "2181b33d2ed3d8bfb292171d3055ad0c")),

                cookiesRequest(stash("cookies", stash("foo_1", "2181b33d2ed3d8bfb292171d3055ad0c"))));
    }

    @Test
    public void shouldSetCookieHeaderOnResponse() throws Exception {
        assertEquals(stash(
                "headers", stash("Set-Cookie", "foo_1=2181b33d2ed3d8bfb292171d3055ad0c")),
                cookiesResponse(stash(
                "headers", stash(),
                "cookies", stash("foo_1", "2181b33d2ed3d8bfb292171d3055ad0c"))));
    }

    @Test
    public void shouldSetStashCookieHeaderOnResponse() throws Exception {
        assertEquals(stash(
                "headers", stash("Set-Cookie", "foo_1=eggs=4;Expires=2011-30-348T08:30:00-0500;Max-Age=5;Secure")),
                cookiesResponse(stash(
                        "headers", stash(),
                        "cookies", stash("foo_1", stash("max-age", 5L, "expires", Dates.date("2011-12-14T13:30:00Z"), "secure", true, "value", stash("eggs", 4))))));
    }

    @Test
    public void wrapsAHandleWithCookies() throws Exception {
        Handler handler = wrapCookies((req) -> {
            Stash cookies = req.get("cookies");
            assertEquals(stash("foo", "bar"), cookies);

            return stash("headers", stash(), "cookies", stash("eggs", "spam"));
        });

        Stash actual = handler.apply(stash(
                "headers", stash("cookie", "foo=bar")));

        assertEquals(stash("headers", stash("Set-Cookie", "eggs=spam")), actual);
    }

    @Test
    public void wrapsAnAsyncHandlerWithCookies() throws Exception {
        AsyncHandler asyncHandler = wrapCookies((req, res, raise) -> {
            Stash cookies = req.get("cookies");
            assertEquals(stash("foo", "bar"), cookies);

            return res.apply(stash("headers", stash(), "cookies", stash("eggs", "spam")));
        });

        Stash actual = asyncHandler.apply(stash(
                "headers", stash("cookie", "foo=bar")), r -> r, (c) -> die(c, "Failed in test"));

        assertEquals(stash("headers", stash("Set-Cookie", "eggs=spam")), actual);
    }
}