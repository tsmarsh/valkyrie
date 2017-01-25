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
import static com.tailoredshapes.valkyrie.servlet.Servlet.servlet;
import static org.junit.Assert.assertEquals;

public class ServletTest {

    private ServletContextHandler context;
    private Server server;

    private Stash response = stash(
            "status", 200,
            "headers", stash("Content-Type", "text/plain"),
            "body", "Hello, World");

    @Before
    public void setUp() throws Exception {

        server = new Server(6666);

        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        server.setHandler(context);
    }

    @Test
    public void shouldCreateAServletFromAHandler() throws Exception {
        HttpServlet servlet = servlet((req) -> response);

        context.addServlet(new ServletHolder(servlet), "/*");

        server.start();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:6666");
        CloseableHttpResponse response = client.execute(get);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("text/plain", response.getFirstHeader("Content-Type").getValue());
        String body = EntityUtils.toString(response.getEntity());
        assertEquals("Hello, World", body);

        server.stop();
    }

    @Test
    public void shouldCreateAServletFromAnAsyncHandler() throws Exception {
        HttpServlet servlet = servlet((request, onSuccess, onError) -> onSuccess.apply(response));

        context.addServlet(new ServletHolder(servlet), "/*");

        server.start();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:6666");
        CloseableHttpResponse response = client.execute(get);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("text/plain", response.getFirstHeader("Content-Type").getValue());
        String body = EntityUtils.toString(response.getEntity());
        assertEquals("Hello, World", body);

        server.stop();
    }
}