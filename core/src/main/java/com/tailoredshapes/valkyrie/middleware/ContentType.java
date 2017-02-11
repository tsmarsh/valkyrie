package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;

import java.util.function.Function;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.UnderBar.hash;
import static com.tailoredshapes.valkyrie.util.MIMEType.extMimeType;
import static com.tailoredshapes.valkyrie.util.Response.contentType;
import static com.tailoredshapes.valkyrie.util.Response.getHeader;

;

public interface ContentType {
    static Stash contentTypeResponse(Stash res, Stash req){
        return contentTypeResponse(res, req, stash());
    }

    static Stash contentTypeResponse(Stash res, Stash req, Stash options) {
        return getHeader(res, "Content-Type").isPresent() ?
                res :
                contentType(res, extMimeType(req.get("uri"), options.get("mime-types", hash())));

    }

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
