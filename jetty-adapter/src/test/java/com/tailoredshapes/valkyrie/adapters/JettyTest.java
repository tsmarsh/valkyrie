package com.tailoredshapes.valkyrie.adapters;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.valkyrie.adapters.Jetty.runJetty;
import static org.junit.Assert.*;

/**
 * Created by tmarsh on 1/20/17.
 */
public class JettyTest {
    @Test
    public void shouldStartAJettyServer() throws Exception {
        Server server = runJetty((req) -> stash(
                "status", 200,
                "headers", stash("Content-Type", "text/plain"),
                "body", "Hello, World"), stash("join?", false));

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