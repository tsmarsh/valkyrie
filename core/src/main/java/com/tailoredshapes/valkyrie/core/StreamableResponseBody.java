package com.tailoredshapes.valkyrie.core;

import com.tailoredshapes.stash.Stash;

import java.io.*;
import java.util.Collection;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.Die.die;
import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.underbar.UnderBar.optionally;
import static com.tailoredshapes.valkyrie.util.Response.getCharset;

public class StreamableResponseBody {

    public static BufferedWriter responseWriter(Stash response, OutputStream outputStream) {
        return optionally(
                getCharset(response),
                (charset) -> writer(outputStream, stash("encoding", charset)),
                () -> writer(outputStream));
    }


    private static BufferedWriter writer(OutputStream outputStream, Stash opts) {
        return new BufferedWriter(new PrintWriter(outputStream));
    }

    private static BufferedWriter writer(OutputStream outputStream) {
        return new BufferedWriter(new PrintWriter(outputStream));
    }

    public static void writeBodyToStream(String body, Stash response, OutputStream outputStream) {
        try (BufferedWriter writer = responseWriter(response, outputStream)) {
            writer.write(body);
        } catch (IOException e) {
            throw die(e, "Could not open response");
        }
    }

    public static void writeBodyToStream(Collection<Object> body, Stash response, OutputStream outputStream) {
        try (BufferedWriter writer = responseWriter(response, outputStream)) {
            body.stream().forEach((s) -> rethrow(() -> writer.write(s.toString())));
        } catch (IOException e) {
            throw die(e, "Could not open response");
        }
    }

    public static void writeBodyToStream(InputStream body, Stash response, OutputStream outputStream) {
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
    }

    public static void writeBodyToStream(File body, Stash response, OutputStream outputStream) {
        writeBodyToStream(rethrow(() -> new FileInputStream(body)), response, outputStream);
    }
}
