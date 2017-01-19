package com.tailoredshapes.valkyrie.core;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.valkyrie.core.StreamableResponseBody.writeBodyToStream;
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
}