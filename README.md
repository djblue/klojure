# klojure

A blueprint bundle for deploying a
[nrepl](https://nrepl.org/nrepl/0.6.0/index.html) server to a
[karaf](https://karaf.apache.org/) container.

## building

To build the klojure jar, do:

    mvn clean install

## installing

To install the jar, do:

    cp target/klojure-0.0.1-SNAPSHOT.jar $DDF_HOME/deploy

## running a repl

To run a repl in the karaf console, do:

    clj:repl

To connect to the nrepl server with [lein](https://leiningen.org/), do:

    lein repl :connect 7888

The nrepl server is also available for clients such as
[cursive](https://cursive-ide.com/) and
[vim-fireplace](https://github.com/tpope/vim-fireplace).
