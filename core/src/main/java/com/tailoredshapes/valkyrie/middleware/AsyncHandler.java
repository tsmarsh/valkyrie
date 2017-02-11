package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.underbar.function.RegularFunctions;

import java.util.function.Consumer;
import java.util.function.Function;

public interface AsyncHandler extends RegularFunctions.TriFunction<Stash, Function<Stash, Stash>, Consumer<Exception>, Stash> {}
