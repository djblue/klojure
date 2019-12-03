(ns klojure.repl.main
  (:require [nrepl.server :as nrepl])
  (:import [org.slf4j LoggerFactory])
  (:gen-class
    :name klojure.repl.Main
    :methods [^:static [start [int] void]
              ^:static [stop [] void]]))

(def server (atom nil))
(def log (LoggerFactory/getLogger "ROOT"))

(defn- start [port]
  (reset! server (nrepl/start-server :port port :bind "0.0.0.0"))
  (.info log "Started nrepl server on port {}" port))

(defn- stop []
  (nrepl/stop-server @server)
  (.info log "Stopped nrepl server"))

(defn -start [port] (start port))
(defn -stop [] (stop))

