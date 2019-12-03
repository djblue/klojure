package klojure.java.devtools;

public class Main {

  private final int port;

  public Main(int port) {
    // We can't run Clojure without this line because no one is setting the class loader
    // and Clojure just blows up, otherwise we could just use the dynamically generated class
    // in main.clj
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

    this.port = port;
  }

  public void start() {
    klojure.repl.Main.start(port);
  }

  public void stop() {
    klojure.repl.Main.stop();
  }
}
