package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.valkyrie.util.Response;

import java.util.Optional;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.UnderBar.hash;
import static com.tailoredshapes.valkyrie.util.MIMEType.extMimeType;
import static com.tailoredshapes.valkyrie.util.Response.contentType;
import static com.tailoredshapes.valkyrie.util.Response.getHeader;


public interface ContentType {
    static Stash contentTypeResponse(Stash res, Stash req){
        return contentTypeResponse(res, req, stash());
    }

    static Stash contentTypeResponse(Stash res, Stash req, Stash options) {
        return getHeader(res, "Content-Type").isPresent() ?
                res :
                contentType(res, extMimeType(req.get("uri"), options.get("mime-types", hash())));

    }
}
