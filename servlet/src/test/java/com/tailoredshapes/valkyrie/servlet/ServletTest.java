package com.tailoredshapes.valkyrie.servlet;

import com.tailoredshapes.underbar.function.RegularFunctions;
import com.tailoredshapes.underbar.function.RegularFunctions.TriConsumer;
import org.junit.Test;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.UnderBar.hash;
import static com.tailoredshapes.valkyrie.servlet.Servlet.makeServiceMethod;

public class ServletTest {

    @Test
    public void shouldCreateAServletFromAHandler() throws Exception {
        TriConsumer<HttpServlet, HttpServletRequest, HttpServletResponse> serviceMethod = makeServiceMethod((request) -> stash(
                "status", 200,
                "headers", hash("Content-Type", "text/plain"),
                "body", "Hello, World"));

    }
}