package com.tailoredshapes.valkyrie.core;

import com.tailoredshapes.stash.Stash;

import java.io.OutputStream;

public interface StreamableResponseBody {
    void writeBodyToStream(String body, Stash response, OutputStream outputStream);
}
