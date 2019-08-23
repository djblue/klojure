(ns klojure.main
  (:require [klojure.repl :as repl])
  (:gen-class
    :name klojure.Main
    :methods [^:static [start [] void]
              ^:static [stop [] void]]))

(defn -start [] (repl/start))
(defn -stop [] (repl/stop))

