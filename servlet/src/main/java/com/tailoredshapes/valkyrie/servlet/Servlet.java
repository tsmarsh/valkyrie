package com.tailoredshapes.valkyrie.servlet;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.underbar.function.RegularFunctions;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.underbar.UnderBar.optional;
import static com.tailoredshapes.underbar.UnderString.commaSep;
import static com.tailoredshapes.valkyrie.core.StreamableResponseBody.writeBodyToStream;
import static java.util.Collections.list;

/**
 * Created by tmarsh on 12/21/16.
 */
public class Servlet {
    private static Stash getHeaders(HttpServletRequest request) {
        Stash headers = new Stash();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> values = request.getHeaders(headerName);
            headers = headers.assoc(headerName.toLowerCase(), commaSep(list(values)));
        }

        return headers;
    }

    private static Optional<Integer> getContentLength(HttpServletRequest request) {
        int contentLength = request.getContentLength();
        return contentLength > 0 ? optional(contentLength) : optional();
    }

    private static Optional<X509Certificate> getClientCert(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        return certs.length > 0 ? optional(certs[1]) : optional();
    }

    public static Stash buildRequestMap(HttpServletRequest request) {
        return stash(
                "server-port", request.getServerPort(),
                "server-name", request.getServerName(),
                "remote-addr", request.getRemoteAddr(),
                "uri", request.getRequestURI(),
                "query-string", request.getQueryString(),
                "scheme", request.getScheme(),
                "request-method", request.getMethod().toLowerCase(),
                "protocol", request.getProtocol(),
                "headers", getHeaders(request),
                "content-type", request.getContentType(),
                "content-length", getContentLength(request),
                "character-encoding", request.getCharacterEncoding(),
                "ssl-client-cert", getClientCert(request),
                "body", rethrow(request::getInputStream)
        );
    }

    public static Stash mergeServletKeys(Stash requestMap, HttpServlet servlet, HttpServletRequest request, HttpServletResponse response) {
        return requestMap.merge(stash(
                "servlet", servlet,
                "servlet-request", request,
                "servlet-response", response,
                "servlet-context", servlet.getServletContext(),
                "servlet-context-path", request.getContextPath()
        ));
    }

    private static HttpServletResponse setHeaders(HttpServletResponse response, Stash headers) {
        headers.toMap().forEach((key, valOrVals) -> {
            if (valOrVals instanceof String) {
                response.setHeader(key, (String) valOrVals);
            } else if (valOrVals instanceof Collection) {
                Collection<String> vals = (Collection<String>) valOrVals;
                for (String val : vals) {
                    response.addHeader(key, val);
                }
            } else if (valOrVals instanceof String[]) {
                for (String val : (String[]) valOrVals) {
                    response.addHeader(key, val);
                }
            }
        });
        return response;
    }

    private static OutputStream makeOutputStream(HttpServletResponse response, AsyncContext context) {
        if (context == null) {
            return rethrow(() -> response.getOutputStream());
        } else {
            return new FilterOutputStream(rethrow(() -> response.getOutputStream())) {
                @Override
                public void close() throws IOException {
                    context.complete();
                    super.close();
                }
            };
        }
    }

    public static HttpServletResponse updateServletResponse(HttpServletResponse response, Stash responseMap) {
        return updateServletResponse(response, null, responseMap);
    }

    public static HttpServletResponse updateServletResponse(HttpServletResponse response, AsyncContext context, Stash responseMap) {
        assert (responseMap != null);
        assert (response != null);

        if (responseMap.contains("status")) {
            response.setStatus(responseMap.i("status"));
        }
        setHeaders(response, responseMap.get("headers"));

        OutputStream os = makeOutputStream(response, context);
        Object body = responseMap.get("body");

        writeBodyToStream(body, responseMap, os);

        return response;
    }

    public static RegularFunctions.TriConsumer<HttpServlet, HttpServletRequest, HttpServletResponse> makeServiceMethod(Function<Stash, Stash> handler) {
        return (servlet, request, response) -> {
            Stash requestMap = buildRequestMap(request);
            requestMap = mergeServletKeys(requestMap, servlet, request, response);
            requestMap = handler.apply(requestMap);
            updateServletResponse(response, requestMap);
        };
    }

    public static RegularFunctions.TriConsumer<HttpServlet, HttpServletRequest, HttpServletResponse> makeServiceMethod(RegularFunctions.TriConsumer<Stash, Function<Stash, HttpServletResponse>, Consumer<Throwable>> handler) {
        return (servlet, request, response) -> {
            AsyncContext context = request.startAsync();
            Stash requestMap = mergeServletKeys(buildRequestMap(request), servlet, request, response);
            handler.accept(requestMap, (respMap) -> updateServletResponse(response, context, respMap), (e) -> rethrow(() -> response.sendError(500, e.getMessage())));
        };
    }

    public static HttpServlet servlet(Function<Stash, Stash> handler) {
        RegularFunctions.TriConsumer<HttpServlet, HttpServletRequest, HttpServletResponse> serviceMethod = makeServiceMethod(handler);
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                serviceMethod.accept(this, req, resp);
            }
        };
    }

    public static HttpServlet servlet(RegularFunctions.TriConsumer<Stash, Function<Stash, HttpServletResponse>, Consumer<Throwable>> handler) {
        RegularFunctions.TriConsumer<HttpServlet, HttpServletRequest, HttpServletResponse> serviceMethod = makeServiceMethod(handler);
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                serviceMethod.accept(this, req, resp);
            }
        };
    }
}
