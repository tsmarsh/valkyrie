package com.tailoredshapes.valkyrie.util;

import org.junit.Test;

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

}