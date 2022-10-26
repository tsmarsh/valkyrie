package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.tailoredshapes.underbar.ocho.Die.dieIfEmpty;
import static com.tailoredshapes.underbar.ocho.Die.rethrow;
import static com.tailoredshapes.underbar.ocho.UnderBar.*;
import static com.tailoredshapes.underbar.ocho.UnderString.join;

public abstract class FormEncodeable {

    private static final Map<Class, BiFunction<?, String, String>> impls = new HashMap<>();

    static {
        extend(String.class, (unencoded, encoding) -> rethrow(() -> URLEncoder.encode(unencoded, encoding)));
        extend(Map.class, (params, encoding) -> {
            Function<Object, String> encode = (x) -> formEncode(x, encoding);
            BiFunction<Object, Object, String> encodeParam = (k, v) -> encode.apply(k) + "=" + encode.apply(v);
            List<String> encodedParams = new ArrayList<>();
            for (Object k : params.keySet()) {
                Object v = params.get(k);

                if (v instanceof Iterable) {
                    Iterable mapV = (Iterable) v;
                    encodedParams.addAll(map(mapV, u -> encodeParam.apply(k, u)));
                } else {
                    encodedParams.add(encodeParam.apply(k, v));
                }
            }
            return join("&", encodedParams);
        });

        extend(Stash.class, (params, encoding) -> {
            Function<Object, String> encode = (x) -> formEncode(x, encoding);
            BiFunction<Object, Object, String> encodeParam = (k, v) -> encode.apply(k) + "=" + encode.apply(v);
            List<String> encodedParams = new ArrayList<>();
            params.map((k,v) -> {
                Object val = params.get(k);

                if (val instanceof Iterable) {
                    Iterable mapV = (Iterable) val;
                    encodedParams.addAll(map(mapV, u -> encodeParam.apply(k, u)));
                } else {
                    encodedParams.add(encodeParam.apply(k, val));
                }

                return v;
            });

            return join("&", encodedParams);
        });
    }


    public static <T> void extend(Class<T> type, BiFunction<T, String, String> impl) {
        synchronized (impls) {
            impls.put(type, impl);
        }
    }

    public static <T> String formEncode(T t, String encoding) {
        BiFunction<T, String, String> formEncoder;

        if (impls.containsKey(t.getClass())) {
            synchronized (impls) {
                formEncoder = (BiFunction<T, String, String>) impls.get(t.getClass());
            }
        } else {
            List<Class> handlers = filter(impls.keySet(), (Class k) -> k.isAssignableFrom(t.getClass()));
            if (isEmpty(handlers)) {
                return formEncode(t.toString(), encoding);
            }
            Class targetClass = first(dieIfEmpty(handlers, () -> "No implementation for " + t.getClass().getName()));
            formEncoder = (BiFunction<T, String, String>) impls.get(targetClass);
        }

        return formEncoder.apply(t, encoding);
    }
}
