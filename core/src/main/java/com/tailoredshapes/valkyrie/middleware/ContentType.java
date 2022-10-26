package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.valkyrie.core.AsyncHandler;
import com.tailoredshapes.valkyrie.core.Handler;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.ocho.UnderBar.hash;
import static com.tailoredshapes.valkyrie.util.MIMEType.extMimeType;
import static com.tailoredshapes.valkyrie.util.Response.contentType;
import static com.tailoredshapes.valkyrie.util.Response.getHeader;


/**
 * "Middleware for automatically adding a content type to response maps."
 */
public interface ContentType {
    /**
     * "Adds a content-type header to response. See: wrap-content-type."
     */
    static Stash contentTypeResponse(Stash res, Stash req){
        return contentTypeResponse(res, req, stash());
    }

    static Stash contentTypeResponse(Stash res, Stash req, Stash options) {
        return getHeader(res, "Content-Type").isPresent() ?
                res :
                contentType(res, extMimeType(req.get("uri"), options.get("mime-types", hash())));

    }

    /**
     * Middleware that adds a content-type header to the response if one is not
     set by the handler. Uses the ring.util.mime-type/ext-mime-type function to
     guess the content-type from the file extension in the URI. If no
     content-type can be found, it defaults to 'application/octet-stream'.
     Accepts the following options:
     :mime-types - a map of filename extensions to mime-types that will be
     used in addition to the ones defined in
     ring.util.mime-types/default-mime-types
     */
    static Handler wrapContentType(Handler handler){
        return wrapContentType(handler, stash());
    }

    static Handler wrapContentType(Handler handler, Stash options){
        return (req) -> contentTypeResponse(handler.apply(req), req, options);
    }

    static AsyncHandler wrapContentType(AsyncHandler handler) {
        return wrapContentType(handler, stash());
    }

    static AsyncHandler wrapContentType(AsyncHandler handler, Stash options){
        return (request, respond, raise) ->
                    handler.apply(
                            request,
                            (response) ->
                                respond.apply(contentTypeResponse(response, request, options)),
                            raise);
    }
}
