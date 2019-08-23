# klojure

[![Build Status](https://travis-ci.org/djblue/klojure.svg?branch=master)](https://travis-ci.org/djblue/klojure)

A blueprint bundle for deploying a
[nrepl](https://nrepl.org/nrepl/0.6.0/index.html) server to a
[karaf](https://karaf.apache.org/) container.

## Installing

To install the bundle in a running karaf instance, do:

    bundle:install https://git.io/fjHIs; start klojure

**NOTE**: you will probably need to disable the DDF Security Manager as
clojure brings along its own classloader which causes problems with
security.

## Running a REPL

To run a repl in the karaf console, do:

    clj:repl

To connect to the nrepl server with [lein](https://leiningen.org/), do:

    lein repl :connect 7888

The nrepl server is also available for clients such as
[cursive](https://cursive-ide.com/) and
[vim-fireplace](https://github.com/tpope/vim-fireplace).

## DDF Clojure API

Klojure exposes a clojure API for [DDF](https://github.com/codice/ddf). It
can be found in the [`catalog.core`](src/main/clojure/catalog/core.clj)
namespace.

Below is an example of the API:

```clojure
(require '[catalog.core :as c])

; query for exisitng metacards
(c/query [[:attribute "id"] :is :like [:text "*"]])

; create a new metacard
(c/create! {:id "" :title "one"})

; delete a metacard by id
(c/delete! "afa5099ccfcb40c3b39bade0148fc342")

; delete all metacards
(->> (c/query [[:attribute "id"] :is :like [:text "*"]])
     (map ::id)
     (apply c/delete!))

; query for all workspaces
(c/query [[:attribute "metacard-tags"] :is :like [:text "workspace"]])
```

## Building

To build the klojure jar, do:

    mvn clean install

To hot deploy a development version, do:

    cp target/klojure-*.jar $DDF_HOME/deploy

## Documentation

 * [API Docs](http://djblue.github.io/klojure/)

## License

Copyright (c) 2019 Chris Badahdah

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.

