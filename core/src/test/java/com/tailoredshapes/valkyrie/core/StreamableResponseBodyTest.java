package com.tailoredshapes.valkyrie.core;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.Die.die;
import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.underbar.IO.file;
import static com.tailoredshapes.underbar.IO.resource;
import static com.tailoredshapes.underbar.IO.responseWriter;
import static com.tailoredshapes.underbar.UnderBar.list;
import static com.tailoredshapes.valkyrie.core.StreamableResponseBody.extend;
import static com.tailoredshapes.valkyrie.core.StreamableResponseBody.writeBodyToStream;
import static com.tailoredshapes.valkyrie.util.Response.getCharset;
import static org.junit.Assert.*;

/**
 * Created by tmarsh on 11/4/16.
 */
public class StreamableResponseBodyTest {


    @Test
    public void canWriteStrings() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Stash response = stash("body", "Hello, World!", "headers", stash());
        writeBodyToStream(response.get("body"), response, output);

        assertEquals("Hello, World!", output.toString());
    }

    @Test
    public void canWriteStringsWithEncoding() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Stash response = stash("body", "Hello, World!", "headers", stash("Content-Type", "text/plain; charset=UTF-16"));
        writeBodyToStream(response.get("body"), response, output);

        assertEquals("Hello, World!", output.toString("UTF-16"));
    }

    @Test
    public void canWriteOutputStreams() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Stash response = stash("body", new ByteArrayInputStream("Hello, World!".getBytes("UTF-8")), "headers", stash());
        writeBodyToStream(response.get("body"), response, output);

        assertEquals("Hello, World!", output.toString("UTF-8"));
    }

    @Test
    public void canWriteCollections() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Stash response = stash("body", list("Hello, ", "World", '!'), "headers", stash());
        writeBodyToStream(response.get("body"), response, output);

        assertEquals("Hello, World!", output.toString("UTF-8"));
    }

    @Test
    public void canWriteFiles() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Stash response = stash("body", file(resource("/test.txt")), "headers", stash());
        writeBodyToStream(response.get("body"), response, output);

        assertEquals("Hello, World!", output.toString("UTF-8"));
    }

    @Test
    public void canBeExtendedArbitrarily() throws Exception {
        extend(URL.class, (body, response, outputStream) -> {
            writeBodyToStream(file(body), response, outputStream);
        });

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Stash response = stash("body", resource("/test.txt"), "headers", stash());
        writeBodyToStream(response.get("body"), response, output);

        assertEquals("Hello, World!", output.toString("UTF-8"));
    }
}