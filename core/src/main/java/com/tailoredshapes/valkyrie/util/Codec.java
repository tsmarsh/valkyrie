package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static com.tailoredshapes.underbar.Die.rethrow;

/**
 * Created by tmarsh on 2/15/17.
 */
public interface Codec {
    /**
     * Encode the supplied value into www-form-urlencoded format, often used in
     * URL query strings and POST request bodies, using the specified encoding.
     * If the encoding is not specified, it defaults to UTF-8
     */
    static String formEncode(Object x) {
        return formEncode(x, "UTF-8");
    }

    static String formEncode(Object x, String encoding) {
        return FormEncodeable.formEncode(x, encoding);
    }

    /**
     * Decode the supplied www-form-urlencoded string using the specified encoding,
     * or UTF-8 by default.
     */
    static String formDecodeString(String encoded, String... encoding) {
        String enc = encoding.length == 0 ? "UTF-8" : encoding[0];
        return rethrow(() -> URLDecoder.decode(encoded, enc));
    }

    static boolean formContainsMap(String form) {
        return form.contains("=");
    }

    /**
     *   Decode the supplied www-form-urlencoded string using the specified encoding,
     *   or UTF-8 by default. If the encoded value is a string, a string is returned.
     *   If the encoded value is a map of parameters, a map is returned.
     * @param encoded
     * @param encoding
     * @return
     */
    static Stash formDecodeStash(String encoded, String... encoding) {
        String enc = encoding.length == 0 ? "UTF-8" : encoding[0];
        String[] params = encoded.split("&");

        Map<String, Object> m = new HashMap<>();

        for (String param : params) {
            if (param.contains("=")) {
                String[] split = param.split("=", 2);
                m.put(formDecodeString(split[0], enc), formDecodeString(split[1], enc));
            }
        }

        return new Stash(m);
    }

}
