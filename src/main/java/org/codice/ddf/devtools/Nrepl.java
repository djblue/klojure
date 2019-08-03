package org.codice.ddf.devtools;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.Keyword;
import clojure.lang.RT;
import clojure.lang.Symbol;

public class Nrepl {

    private static final Logger LOGGER = LoggerFactory.getLogger(Nrepl.class);

    static final int PORT = 7888;

    static final String BIND = "0.0.0.0";

    private Object server;

    public Nrepl() {
        Thread.currentThread()
                .setContextClassLoader(getClass().getClassLoader());

        RT.var("clojure.core", "require")
                .invoke(Symbol.intern("nrepl.server"));
    }

    public void start() {
        List<Object> args = new ArrayList<>();

        args.add(Keyword.intern("port"));
        args.add(PORT);
        args.add(Keyword.intern("bind"));
        args.add(BIND);

        server = RT.var("nrepl.server", "start-server")
                .applyTo(RT.seq(args));

        LOGGER.info("Started nrepl server on port {}", PORT);
    }

    public void stop() {
        List<Object> args = new ArrayList<>();

        args.add(server);

        RT.var("nrepl.server", "stop-server")
                .applyTo(RT.seq(args));

        LOGGER.info("Stopped nrepl server on port {}", PORT);
    }
}
