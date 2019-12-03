(ns klojure.osgi.core
  "The osgi.core namespace is all about raw transforms between Java data structures
  provided by Karaf and the Clojure data structures used throughout the other
  namespaces. Several key data structures are provided:
  - Bundle definition
  - Feature definition"
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint print-table]]
            [clojure.reflect :as r])
  (:import [org.osgi.framework FrameworkUtil Bundle]
           [org.apache.felix.utils.manifest Clause Parser]
           [clojure.lang RT]
           [java.util Collections]))

(def bundle-states
  "Enumeration of bundle states."
  {Bundle/ACTIVE      :active
   Bundle/INSTALLED   :installed
   Bundle/RESOLVED    :resolved
   Bundle/STOPPING    :stopping
   Bundle/UNINSTALLED :uninstalled
   Bundle/STARTING    :starting})

(defn- bundle-cxt
  "Returns the bundle context for the nREPL bundle."
  []
  (.. FrameworkUtil (getBundle RT) getBundleContext))

(defn- methods-of
  "Attempts to use Java's reflection facilities to get
  string representations of function signatures. Takes
  any Java object."
  [x]
  (->> (.getMethods (class x))
       (map #(.toGenericString %))))

(defn- dict->map [dict]
  (zipmap
    (map (comp keyword str/lower-case)
         (Collections/list (. dict keys)))
    (Collections/list (.elements dict))))

(defn- pair [p]
  (let [k (keyword (.getName p)) v (.getValue p)]
    [k
     (case k
       (:uses :bundle-classpath :embed-dependency)
       (str/split v #",")
       v)]))

(defn- clause->map
  [clause]
  [(.getName clause)
   (into {} (map pair
                 (concat (.getDirectives clause)
                         (.getAttributes clause))))])

(defn- parse-header
  [headers k]
  (let [header (get headers k)]
    (assoc headers k (into {} (map clause->map (Parser/parseHeader header))))))

(defn- parse-headers
  [headers]
  (-> headers
      (parse-header :export-package)
      (parse-header :export-service)
      (parse-header :import-package)
      (parse-header :require-capability)
      (parse-header :embedded-artifacts)))

(defn- bundle->map
  [bundle]
  {:id       (.getBundleId bundle)
   :name     (.getSymbolicName bundle)
   :version  (.getVersion bundle)
   :headers  (-> (.getHeaders bundle) dict->map parse-headers)
   :location (.getLocation bundle)
   :state    (get bundle-states (.getState bundle) :unknown)
   :modified (.getLastModified bundle)
   :fragment (.isFragment bundle)})

; Public operational API
(defn start! [bundle] (.start bundle))
(defn stop! [bundle] (.stop bundle))
(defn uninstall! [bundle] (.uninstall bundle))
(defn shutdown! [] (-> (bundle-cxt) (. getBundle 0) (. stop)))

; Services
(defn get-services [name]
  (let [ctx (bundle-cxt)]
    (->> (. ctx (getAllServiceReferences name nil))
         (map #(. ctx (getService %))))))

; Features
(defn get-features-service []
  (-> "org.apache.karaf.features.FeaturesService" get-services first))
(defn list-repositories [] (.listRepositories (get-features-service)))
(defn list-features [] (->> (list-repositories) (mapcat #(.getFeatures %))))

(defn- bundle-list
  "Retrieves all bundles from the local context."
  []
  (.. (bundle-cxt) getBundles))

(defn bundles
  "Get all bundles installed in Karaf."
  []
  (->> (bundle-list)
       (map bundle->map)))

(defn get-cdm-bundle
  "Fetches the Content Directory Monitor bundle. Useful
  for examples and testing."
  []
  (->> (bundles)
       (filter #(.contains (:name %) "directorymonitor"))
       (first)))

(defn get-klojure-bundle
  "Fetches the Klojure bundle. Useful for examples and
  testing."
  []
  (->> (bundles)
       (filter #(.contains (:name %) "klojure"))
       (first)))

(comment
  "Sample invocation that just lists all the bundles."
  (bundles))

(comment
  "Sample invocation that prints a table of resolved bundles."
  (->> (bundle-list)
       (map bundle->map)
       (filter #(-> % :state (= :resolved)))
       (print-table [:id :state :version :name])))

(comment
  "Sample invocation that uses reflection to get valid operations on a bundle."
  (->> (bundle-list) first methods-of))

(comment
  "Sample invocation that uses Clojure's bean mapping to datafy features."
  (->> (list-features)
       (filter #(.. % getName (contains "admin-ui")))
       (map bean)))