(ns klojure.repl
  (:require [nrepl.server :as nrepl])
  (:import [org.slf4j LoggerFactory]))

(def server (atom nil))

(def log (LoggerFactory/getLogger "ROOT"))

(defn start []
  (let [port 7888]
    (reset! server (nrepl/start-server :port port :bind "0.0.0.0"))
    (.info log "Started nrepl server on port {}" port)))

(defn stop []
  (nrepl/stop-server @server)
  (.info log "Stopped nrepl server"))

