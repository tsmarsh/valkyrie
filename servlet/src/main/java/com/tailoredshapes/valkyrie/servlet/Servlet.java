package com.tailoredshapes.valkyrie.servlet;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.valkyrie.core.AsyncHandler;

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
public interface Servlet {
    static Stash getHeaders(HttpServletRequest request) {
        Stash headers = new Stash();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> values = request.getHeaders(headerName);
            headers = headers.assoc(headerName.toLowerCase(), commaSep(list(values)));
        }

        return headers;
    }

    static Optional<Integer> getContentLength(HttpServletRequest request) {
        int contentLength = request.getContentLength();
        return contentLength > 0 ? optional(contentLength) : optional();
    }

    static Optional<X509Certificate> getClientCert(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        return certs != null && certs.length > 0 ? optional(certs[1]) : optional();
    }

    static Stash buildRequestMap(HttpServletRequest request) {
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

    static Stash mergeServletKeys(Stash requestMap, HttpServlet servlet, HttpServletRequest request, HttpServletResponse response) {
        return requestMap.merge(stash(
                "servlet", servlet,
                "servlet-request", request,
                "servlet-response", response,
                "servlet-context", servlet.getServletContext(),
                "servlet-context-path", request.getContextPath()
        ));
    }

    static HttpServletResponse setHeaders(HttpServletResponse response, Stash headers) {
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

    static OutputStream makeOutputStream(HttpServletResponse response, AsyncContext context) {
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

    static Stash updateServletResponse(HttpServletResponse response, Stash responseMap) {
        return updateServletResponse(response, null, responseMap);
    }

    static Stash updateServletResponse(HttpServletResponse response, AsyncContext context, Stash responseMap) {
        assert (responseMap != null);
        assert (response != null);

        if (responseMap.contains("status")) {
            response.setStatus(responseMap.i("status"));
        }
        setHeaders(response, responseMap.get("headers"));

        OutputStream os = makeOutputStream(response, context);
        Object body = responseMap.get("body");

        writeBodyToStream(body, responseMap, os);

        return responseMap;
    }

    static ServiceMethod makeServiceMethod(Function<Stash, Stash> handler) {
        return (servlet, request, response) -> {
            Stash requestMap = buildRequestMap(request);
            requestMap = mergeServletKeys(requestMap, servlet, request, response);
            requestMap = handler.apply(requestMap);
            updateServletResponse(response, requestMap);
        };
    }

    static ServiceMethod makeServiceMethod(AsyncHandler handler) {
        return (servlet, request, response) -> {
            AsyncContext context = request.startAsync();

            handler.apply(
                    mergeServletKeys(
                            buildRequestMap(request),
                            servlet,
                            request,
                            response),
                    (Stash respMap) -> updateServletResponse(response, context, respMap),
                    (Throwable e) -> rethrow(() -> {
                                response.sendError(500, e.getMessage());
                                context.complete();
                                return stash();
                            }));
        };
    }

    static HttpServlet servlet(Function<Stash, Stash> handler) {
        ServiceMethod serviceMethod = makeServiceMethod(handler);
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                serviceMethod.accept(this, req, resp);
            }
        };
    }

    static HttpServlet servlet(AsyncHandler handler) {
        ServiceMethod serviceMethod = makeServiceMethod(handler);
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                serviceMethod.accept(this, req, resp);
            }
        };
    }
}
