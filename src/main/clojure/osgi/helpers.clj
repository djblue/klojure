(ns osgi.helpers
  (:require [clojure.string :as str])
  (:import (org.osgi.framework FrameworkUtil Bundle)
           (org.codice.ddf.devtools Nrepl)
           (java.util Collections)
           (clojure.lang MapEntry)))

(def states {Bundle/ACTIVE      :active
             Bundle/INSTALLED   :installed
             Bundle/RESOLVED    :resolved
             Bundle/STOPPING    :stopping
             Bundle/UNINSTALLED :uninstalled
             Bundle/STARTING    :starting})

(def mapping {:id       #(. % getBundleId)
              :name     #(. % getSymbolicName)
              :headers  #(into {}
                               (map vector
                                    (map (comp keyword str/lower-case)
                                         (Collections/list (. (. % getHeaders) keys)))
                                    (Collections/list (. (. % getHeaders) elements))))
              :location #(. % getLocation)
              :state    #(get states (. % getState) :unknown)})

(defprotocol BundleProtocol
  "Protocol for managing bundles"
  (start! [this] "Start a bundle.")
  (stop! [this] "Stop a bundle.")
  (uninstall! [this] "Uninstall a bundle."))

(deftype BundleWrapper [bundle]
  clojure.lang.IPersistentMap
  (assoc [this k v] this)
  (assocEx [this k v] this)
  (without [this k] this)

  java.lang.Iterable
  (iterator [_] (seq bundle))

  clojure.lang.Associative
  (containsKey [_ k] (contains? mapping k))
  (entryAt [_ k] (MapEntry. k ((get mapping k (fn [_] nil)) bundle)))

  clojure.lang.IPersistentCollection
  (count [_] (count mapping))
  (cons [this o] this)
  (empty [this] this)
  (equiv [_ o] (= o bundle))

  clojure.lang.Seqable
  (seq [_] (map (fn [[k f]] (MapEntry. k (f bundle))) mapping))

  clojure.lang.ILookup
  (valAt [_ k] ((get mapping k (fn [_] nil)) bundle))
  (valAt [_ k not-found] ((get mapping k (fn [_] not-found)) bundle))

  BundleProtocol
  (start! [_] (. bundle start))
  (stop! [_] (. bundle stop))
  (uninstall! [_] (. bundle uninstall)))

(defn- bundle-cxt []
  (.. FrameworkUtil (getBundle Nrepl) getBundleContext))

(defn bundle-list []
  (map #(BundleWrapper. %) (. (bundle-cxt) getBundles)))

(defn bundle-by-id [id]
  (first (filter #(= id (:id %)) (bundle-list))))

(defn shutdown! []
  (-> (bundle-cxt) (. getBundle 0) (. stop)))