package com.tailoredshapes.valkyrie.servlet;

import com.tailoredshapes.stash.Stash;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServlet;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.IO.file;
import static com.tailoredshapes.underbar.IO.resource;
import static com.tailoredshapes.underbar.UnderBar.list;
import static com.tailoredshapes.valkyrie.servlet.Servlet.servlet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServletTest {

    private ServletContextHandler context;
    private Server server;

    private Stash response = stash(
            "status", 200,
            "headers", stash("Content-Type", "text/plain"),
            "body", "Hello, World");

    private Stash fileResponse = stash(
            "status", 200,
            "headers", stash("Content-Type", "text/plain"),
            "body", file(resource("/hello.txt")));

    private Stash streamResponse = stash(
            "status", 200,
            "headers", stash("Content-Type", "text/plain"),
            "body", this.getClass().getResourceAsStream("/hello.txt"));

    private Stash collectionResponse = stash(
            "status", 200,
            "headers", stash("Content-Type", "text/plain"),
            "body", list("1", 2, "Hello, World"));

    @Before
    public void setUp() throws Exception {
        server = new Server(6666);

        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        server.setHandler(context);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void shouldCreateAServletFromAHandler() throws Exception {
        HttpServlet servlet = servlet((req) -> response);

        context.addServlet(new ServletHolder(servlet), "/*");

        getResource();
    }

    @Test
    public void shouldCreateAServletFromAFileHandler() throws Exception {
        HttpServlet servlet = servlet((req) -> fileResponse);

        context.addServlet(new ServletHolder(servlet), "/*");

        getResource();
    }

    @Test
    public void shouldCreateAServletFromAStreamHandler() throws Exception {
        HttpServlet servlet = servlet((req) -> streamResponse);

        context.addServlet(new ServletHolder(servlet), "/*");

        getResource();
    }

    @Test
    public void shouldCreateAServletFromACollectionHandler() throws Exception {
        HttpServlet servlet = servlet((req) -> collectionResponse);

        context.addServlet(new ServletHolder(servlet), "/*");

        getResource();
    }

    @Test
    public void shouldReturn500WhenFileIsMissing() throws Exception {
        HttpServlet servlet = servlet((req) -> stash(
                "status", 200,
                "headers", stash("Content-Type", "text/plain"),
                "body", this.getClass().getResourceAsStream("/missing.txt")));

        context.addServlet(new ServletHolder(servlet), "/*");

        server.start();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:6666");
        CloseableHttpResponse response = client.execute(get);
        assertEquals(500, response.getStatusLine().getStatusCode());

        server.stop();
    }

    @Test
    public void shouldCreateAServletFromAnAsyncHandler() throws Exception {
        HttpServlet servlet = servlet((request, onSuccess, onError) -> onSuccess.apply(response));

        context.addServlet(new ServletHolder(servlet), "/*");

        getResource();
    }

    @Test
    public void shouldCreateAServletFromAnAsyncHandlerThatFails() throws Exception {
        HttpServlet servlet = servlet((request, onSuccess, onError) -> onError.accept(new RuntimeException("failure")));

        context.addServlet(new ServletHolder(servlet), "/*");

        server.start();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:6666");
        CloseableHttpResponse response = client.execute(get);
        assertEquals(500, response.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(response.getEntity());
        assertTrue(body.contains("failure"));

        server.stop();
    }

    private void getResource() throws Exception {
        server.start();
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:6666");
        CloseableHttpResponse response = client.execute(get);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("text/plain", response.getFirstHeader("Content-Type").getValue());
        String body = EntityUtils.toString(response.getEntity());
        assertTrue(body.contains("Hello, World"));
    }
}