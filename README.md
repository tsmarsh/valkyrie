# Valkyrie

[![Build Status](https://travis-ci.org/tsmarsh/valkyrie.svg?branch=master)](https://travis-ci.org/tsmarsh/valkyrie)
[![codecov](https://codecov.io/gh/tsmarsh/valkyrie/branch/master/graph/badge.svg)](https://codecov.io/gh/tsmarsh/valkyrie)


A Java 8 port of Clojure's [Ring](https://github.com/ring-clojure/ring) that uses [UnderBar](https://github.com/tsmarsh/UnderBar) for much skookum.

So what is this all about? Clojure, like all good lisps recognises the 3 types of closures:

1. Data / Structs: a closure with entry point of execution
2. Functions: a closure with 1 entry point of execution
3. Objects: a closure with N entry points of exetution.

The more of your problem you can express as data, rather than functions, the easier your life gets. 
Data doesn't go wrong. 
It can be wrong, it can be incorrectly manipulated, but none of that is the data's fault. You can test the structure of the data, but there is no logic to test.


Functions, at least in the small, are easier to understand. 
They do one thing and one thing well. Closures go in, Closures come out. They are easier to test than objects, especially if they're pure (no side effects or state).

Objects are the most versatile. Being able to group functions and state together is our most powerful weapon. But with great power comes great maintenance headache. Objects by their very nature have more that can go wrong. They're harder to reason about in multithreaded environments. They're great but we should use less of them. 

So to recap, a good code base should be:

Data > Functions > Objects

In Java, we're invariably taught that 'Everything is an Object...(apart from primatives)'. 

This has meant that most Java developers use objects for everything. EVERYTHING. 

A server definition is expressed as an object (a Servlet) that operates on other objects (a Request and a Response) by passing them other objects (often Strings and Streams). 

Ring, and now Valkrie, express them as functions (a handler) that accept data and return data. 

```$java
Function<Stash, Stash> handler = (req) -> stash(
                                                "status", 200,
                                                "headers", stash("Content-Type", "text/plain", 
                                                                 "Cookie", list("foo=5", "moo=cow")),
                                                "body", file(resource("/hello.txt")));
```

We can now take that handler and turn it into a servlet:

```$java
HTTPServer servlet = servlet((req) -> stash(
                                           "status", 200,
                                           "headers", stash("Content-Type", "text/plain", 
                                                            "Cookie", list("foo=5", "moo=cow")),
                                           "body", file(resource("/hello.txt")));
```

Which you can plug into a JEE Servlet application, or maybe you just want an embedded Jetty Server:

```$java
Server server = runJetty((req) -> stash(
                                       "status", 200,
                                       "headers", stash("Content-Type", "text/plain", 
                                                        "Cookie", list("foo=5", "moo=cow")),
                                       "body", file(resource("/hello.txt")));

server.start();
```

Because we're dealing predominantly with data, wrapped in a simple function, manipulating that data into a servlet or Jetty Handler is trivial.

The result is that the only thing that developers are writing is simple, re-usable functions.


## Todo

* ~Core library~
* ~Sync Servlets~
* ~Async Servlets~
* ~Sync Jetty~
* ~HTTPS Jetty~
* Async Jetty
* Middleware


