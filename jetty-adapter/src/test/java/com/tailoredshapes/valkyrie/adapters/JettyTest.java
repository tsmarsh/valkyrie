package com.tailoredshapes.valkyrie.adapters;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.IO.file;
import static com.tailoredshapes.underbar.IO.resource;
import static com.tailoredshapes.valkyrie.adapters.Jetty.runJetty;
import static org.junit.Assert.assertEquals;

/**
 * Created by tmarsh on 1/20/17.
 */
public class JettyTest {

    @Test
    public void shouldStartAJettyServer() throws Exception {
        Server server = runJetty((req) ->
                        stash(
                                "status", 200,
                                "headers", stash("Content-Type", "text/plain"),
                                "body", "Hello, World"),
                        stash("join?", false, "port", 6666));

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
    public void shouldStartASSLJettyServer() throws Exception {
        Server server = runJetty((req) ->
                        stash(
                                "status", 200,
                                "headers", stash("Content-Type", "text/plain"),
                                "body", "Hello, World"),
                stash(
                        "join?", false,
                        "port", 6699,
                        "ssl?", true,
                        "ssl-port", 5533,
                        "keystore", file(resource("/keystore")).getAbsolutePath(),
                        "key-password", "p@ssW0rd!",
                        "host", "localhost"));

        server.start();

        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {  }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());

        CloseableHttpClient client = HttpClients.custom()
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setSSLContext(sc)
                .build();

        HttpGet get = new HttpGet("https://localhost:5533");
        CloseableHttpResponse response = client.execute(get);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("text/plain", response.getFirstHeader("Content-Type").getValue());
        String body = EntityUtils.toString(response.getEntity());
        assertEquals("Hello, World", body);

        server.stop();
    }
}