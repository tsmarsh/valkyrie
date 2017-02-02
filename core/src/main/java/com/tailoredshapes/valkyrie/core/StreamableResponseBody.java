package com.tailoredshapes.valkyrie.core;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.underbar.function.RegularFunctions;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.Die.*;
import static com.tailoredshapes.underbar.IO.responseWriter;
import static com.tailoredshapes.underbar.UnderBar.filter;
import static com.tailoredshapes.underbar.UnderBar.first;
import static com.tailoredshapes.valkyrie.util.Response.getCharset;

public abstract class StreamableResponseBody {

    private static final Map<Class, RegularFunctions.TriConsumer<?, Stash, OutputStream>> impls = new HashMap<>();

    static {
        extend(String.class, (body, response, outputStream) -> {
            try (BufferedWriter writer = responseWriter(outputStream, stash("encoding", getCharset(response)))) {
                writer.write(body);
            } catch (IOException e) {
                die(e, "Could not open response");
            }
        });

        extend(Collection.class, (body, response, outputStream) -> {
            try (BufferedWriter writer = responseWriter(outputStream, stash("encoding", getCharset(response)))) {
                body.stream().forEach((s) -> rethrow(() -> writer.write(s.toString())));
            } catch (IOException e) {
                die(e, "Could not open response");
            }
        });

        extend(InputStream.class, (body, response, outputStream) -> {
            try (InputStream in = body) {
                try (OutputStream out = outputStream) {
                    byte[] buffy = new byte[1024];
                    int len;
                    while ((len = in.read(buffy)) != -1) {
                        out.write(buffy, 0, len);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        extend(File.class, (body, response, outputStream) -> {
            writeBodyToStream(rethrow(() -> new FileInputStream(body)), response, outputStream);
        });
    }


    public static <T> void extend(Class<T> type, RegularFunctions.TriConsumer<T, Stash, OutputStream> impl) {
        synchronized (impls) {
            impls.put(type, impl);
        }
    }

    public static <T> void writeBodyToStream(T t, Stash response, OutputStream outputStream){
        RegularFunctions.TriConsumer<T, Stash, OutputStream> stashOutputStreamTriConsumer;

        if(impls.containsKey(t.getClass())){
            synchronized (impls){
                stashOutputStreamTriConsumer = (RegularFunctions.TriConsumer<T, Stash, OutputStream>) impls.get(t.getClass());
            }
        } else {
            List<Class> handlers = filter(impls.keySet(), (Class k) -> k.isAssignableFrom(t.getClass()));
            Class targetClass = first(dieIfEmpty(handlers, () -> "No implementation for " + t.getClass().getName()));
            stashOutputStreamTriConsumer = (RegularFunctions.TriConsumer<T, Stash, OutputStream>) impls.get(targetClass);
        }

        stashOutputStreamTriConsumer.accept(t, response, outputStream);
    }
}
