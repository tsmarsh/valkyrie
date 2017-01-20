package com.tailoredshapes.valkyrie.servlet;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Test;

import javax.servlet.http.HttpServlet;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.valkyrie.servlet.Servlet.servlet;
import static org.junit.Assert.assertEquals;

public class ServletTest {

    @Test
    public void shouldCreateAServletFromAHandler() throws Exception {
        Server server = new Server(6666);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        server.setHandler(context);


        HttpServlet servlet = servlet((req) -> stash(
                "status", 200,
                "headers", stash("Content-Type", "text/plain"),
                "body", "Hello, World"));

        context.addServlet(new ServletHolder(servlet), "/*");


        server.start();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("http://localhost:6666");
        CloseableHttpResponse response = client.execute(get);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("text/plain", response.getFirstHeader("Content-Type").getValue());
        String body = EntityUtils.toString(response.getEntity());
        assertEquals("Hello, World", body);

    }
}