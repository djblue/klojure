package org.codice.ddf.devtools;

public class Main {
    public Main() {
        Thread.currentThread()
                .setContextClassLoader(getClass().getClassLoader());
    }

    public void start() {
        klojure.Main.start();
    }

    public void stop() {
        klojure.Main.stop();
    }
}
