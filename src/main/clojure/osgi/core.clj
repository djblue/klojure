(ns osgi.core
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint print-table]]
            [clojure.reflect :as r])
  (:import [org.osgi.framework FrameworkUtil Bundle]
           [org.apache.felix.utils.manifest Clause Parser]
           [org.codice.ddf.devtools Nrepl]
           [java.util Collections]))

(defn methods-of [x]
  (->> (.getMethods (class x)) (map #(.toGenericString %))))

(def states {Bundle/ACTIVE      :active
             Bundle/INSTALLED   :installed
             Bundle/RESOLVED    :resolved
             Bundle/STOPPING    :stopping
             Bundle/UNINSTALLED :uninstalled
             Bundle/STARTING    :starting})

(defn dict->map [dict]
  (into {}
        (map vector
             (map (comp keyword str/lower-case)
                  (Collections/list (. dict keys)))
             (Collections/list (.elements dict)))))

(defn pair [p]
  (let [k (keyword (.getName p)) v (.getValue p)]
    [k
     (case k
       (:uses :bundle-classpath :embed-dependency)
       (str/split v #",")
       v)]))

(defn clause->map [clause]
  [(.getName clause)
   (into {} (map pair (concat (.getDirectives clause)
                              (.getAttributes clause))))])

(defn parse-header [headers k]
  (let [header (get headers k)]
    (assoc headers k (into {} (map clause->map (Parser/parseHeader header))))))

(defn parse-headers [headers]
  (-> headers
      (parse-header :export-package)
      (parse-header :export-service)
      (parse-header :import-package)
      (parse-header :require-capability)
      (parse-header :embedded-artifacts)))

(defn bundle->map [bundle]
  {:id       (.getBundleId bundle)
   :name     (.getSymbolicName bundle)
   :version  (.getVersion bundle)
   :headers  (-> (.getHeaders bundle) dict->map parse-headers)
   :location (.getLocation bundle)
   :state    (get states (.getState bundle) :unknown)
   :modified (.getLastModified bundle)
   :fragment (.isFragment bundle)})

(defn start! [bundle] (.start bundle))
(defn stop! [bundle] (.stop bundle))
(defn uninstall! [bundle] (.uninstall bundle))

(defn bundle-cxt []
  (.. FrameworkUtil (getBundle Nrepl) getBundleContext))

(defn shutdown! []
  (-> (bundle-cxt) (. getBundle 0) (. stop)))

(defn bundle-list []
  (.. (bundle-cxt) getBundles))

(defn bundle-by-location [coor]
  (->> (bundle-list) (filter #(-> % (.. getLocation) (= coor))) first))

(defn get-services [name]
  (let [ctx (bundle-cxt)]
    (->> (. ctx (getAllServiceReferences name nil))
         (map #(. ctx (getService %))))))

(defn get-features-service []
  (-> "org.apache.karaf.features.FeaturesService" get-services first))

(defn list-repositories []
  (.listRepositories (get-features-service)))

(defn list-features []
  (->> (list-repositories) (mapcat #(.getFeatures %))))

(comment
  (->> (bundle-list)
       (map bundle->map)
       (filter #(-> % :state (= :resolved)))
       (print-table [:id :state :version :name]))

  (->> (bundle-list) first methods-of)

  (->> (list-features)
       (filter #(.. % getName (contains "admin-ui")))
       (map bean)))
