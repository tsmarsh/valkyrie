package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import java.util.Optional;

import static com.tailoredshapes.stash.Stash.stash;
import static org.junit.Assert.*;
import static com.tailoredshapes.valkyrie.util.Response.*;

/**
 * Created by tmarsh on 10/25/16.
 */
public class ResponseTest {

    @Test
    public void directoryTraversalTest() throws Exception {
        assertTrue(isDirectoryTraversal("../"));
        assertTrue(isDirectoryTraversal("\\.."));
        assertFalse(isDirectoryTraversal("/path/to/nowhere"));
    }

    @Test
    public void canParseACharacterSetOutOfAContentHeader() throws Exception {
        Stash response = stash("headers", stash("Content-Type", "text/plain; charset=UTF-16"));
        assertEquals("UTF-16", getCharset(response).get());
    }
}