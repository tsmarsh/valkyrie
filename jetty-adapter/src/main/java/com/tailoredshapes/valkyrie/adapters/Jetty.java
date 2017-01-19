package com.tailoredshapes.valkyrie.adapters;

import com.tailoredshapes.stash.Stash;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyStore;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.tailoredshapes.underbar.Die.die;
import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.valkyrie.servlet.Servlet.buildRequestMap;
import static com.tailoredshapes.valkyrie.servlet.Servlet.updateServletResponse;


/**
 * Created by tmarsh on 1/19/17.
 */
public class Jetty {
    private static AbstractHandler proxyHandler(Function<Stash, Stash> handler) {
        return new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                Stash requestMap = buildRequestMap(request);
                Stash responseMap = handler.apply(requestMap);
                updateServletResponse(response, responseMap);
                baseRequest.setHandled(true);
            }
        };
    }

    private static HttpConfiguration httpConfig(Stash options) {
        HttpConfiguration conf = new HttpConfiguration();
        conf.setSendDateHeader(options.get("send-date-header?", true));
        conf.setOutputBufferSize(options.get("output-buffer-size", 32768));
        conf.setRequestHeaderSize(options.get("request-header-size", 8192));
        conf.setResponseHeaderSize(options.get("response-header-size", 8192));
        conf.setSendServerVersion(options.get("send-server-version?", true));
        return conf;
    }

    private static ServerConnector httpConnector(Server server, Stash options) {
        HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig(options));
        ServerConnector serverConnector = new ServerConnector(server, connectionFactory);
        serverConnector.setPort(options.get("port", 80));
        serverConnector.setHost(options.get("host"));
        serverConnector.setIdleTimeout(options.get("max-idle-time", 200000));
        return serverConnector;
    }

    private static SslContextFactory sslContextFactory(Stash options) {
        SslContextFactory context = new SslContextFactory();
        Object keystore = options.get("keystore");
        if (keystore instanceof String) {
            context.setKeyStorePath((String) keystore);
        }
        if (keystore instanceof KeyStore) {
            context.setKeyStore((KeyStore) keystore);
        }

        context.setKeyStorePassword(options.get("key-password"));
        Object truststore = options.get("truststore");
        if (truststore instanceof String) {
            context.setTrustStorePath((String) truststore);
        }
        if (truststore instanceof KeyStore) {
            context.setTrustStore((KeyStore) truststore);
        }
        if (options.contains("trust-password")) {
            context.setTrustStorePassword(options.get("trust-password"));
        }
        String clientAuth = options.get("client-auth");
        switch (clientAuth) {
            case "need":
                context.setNeedClientAuth(true);
                break;
            case "want":
                context.setWantClientAuth(true);
        }
        if(options.contains("exclude-ciphers")){
            context.addExcludeCipherSuites(options.get("exclude-ciphers"));
        }

        if(options.contains("exclude-protocols")){
            context.addExcludeProtocols(options.get("excludeo-protocols"));
        }

        return context;
    }

    private static ServerConnector sslConnector(Server server, Stash options){
        Integer sslPort = options.get("ssl-port", 443);
        HttpConfiguration config = httpConfig(options);
        config.setSecureScheme("https");
        config.setSecurePort(sslPort);
        config.addCustomizer(new SecureRequestCustomizer());
        HttpConnectionFactory httpFactory = new HttpConnectionFactory(config);
        SslConnectionFactory sslFactory = new SslConnectionFactory(sslContextFactory(options), "http/1.1");

        ServerConnector serverConnector = new ServerConnector(server, sslFactory, httpFactory);
        serverConnector.setPort(sslPort);
        serverConnector.setHost(options.get("host"));
        serverConnector.setIdleTimeout(options.get("max-idle-time", 200000));
        return serverConnector;
    }

    private static ThreadPool createThreadPool(Stash options){
        QueuedThreadPool pool = new QueuedThreadPool(options.get("max-thread", 50));
        pool.setMinThreads(options.get("min-threads", 8));
        pool.setDaemon(options.get("daemon?", false));
        return pool;
    }

    private static Server createServer(Stash options){
        Server server = new Server(createThreadPool(options));
        if(options.get("http?")){
            server.addConnector(httpConnector(server, options));
        }
        if(options.get("ssl?", false) || options.contains("ssl-port")){
            server.addConnector(sslConnector(server, options));
        }
        return server;
    }

    /**
     *   "Start a Jetty webserver to serve the given handler according to the
     supplied stash of options:

     "configurator"         - a function called with the Jetty Server instance
     "async?"               - if true, treat the handler as asynchronous
     "port"                 - the port to listen on (defaults to 80)
     "host"                 - the hostname to listen on
     "join?"                - blocks the thread until server ends (defaults to true)
     "daemon?"              - use daemon threads (defaults to false)
     "http?"                - listen on :port for HTTP traffic (defaults to true)
     "ssl?"                 - allow connections over HTTPS
     "ssl-port"             - the SSL port to listen on (defaults to 443, implies ssl? is true)
     "exclude-ciphers"      - When :ssl? is true, exclude these cipher suites
     "exclude-protocols"    - When :ssl? is true, exclude these protocols
     "keystore"             - the keystore to use for SSL connections
     "key-password"         - the password to the keystore
     "truststore"           - a truststore to use for SSL connections
     "trust-password"       - the password to the truststore
     "max-threads"          - the maximum number of threads to use (default 50)
     "min-threads"          - the minimum number of threads to use (default 8)
     "max-idle-time"        - the maximum idle time in milliseconds for a connection(default 200000)
     "client-auth"          - SSL client certificate authenticate, may be set to "need","want" or "none" (defaults to :none)
     "send-date-header?"    - add a date header to the response (default true)
     "output-buffer-size"   - the response body buffer size (default 32768)
     "request-header-size"  - the maximum size of a request header (default 8192)
     "response-header-size" - the maximum size of a response header (default 8192)
     "send-server-version?" - add Server header to HTTP response (default true)"
     * @param handler
     * @param options
     * @return
     */

    public static Server runJetty(Function<Stash, Stash> handler, Stash options){
        Server server = createServer(options.remove("configurator"));
        server.setHandler(proxyHandler(handler));
        if(options.contains("configurator")){
            Consumer<Server> configurator = options.get("configurator");
            configurator.accept(server);
        }
        try{
            server.start();
            if(options.get("join?", true)){
                server.join();

            }
        } catch (Exception e) {
            rethrow(server::stop);
            die(e, e.getMessage());
        }
        return server;
    }
}