package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;

import java.util.function.Function;

public interface Handler extends Function<Stash, Stash> {}
