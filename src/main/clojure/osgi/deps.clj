(ns osgi.deps
  (:require [osgi.core :as osgi]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [dorothy.core :as dot]
            [dorothy.jvm :refer [save!]]))

(defn render [graph]
  (let [png "/tmp/render.png"]
    (-> graph dot/dot (save! png {:format :png :layout :circo}))
    (sh "open" png)
    (sh "rm" png)))

(defn get-deps [bundles]
  (->> bundles
       (mapcat (fn [bundle]
                 (map #(-> [(first %) (:name bundle)])
                      (get-in bundle [:headers :export-package]))))
       (into {})))

(defn package-deps []
  (let [bundles (->> (osgi/bundle-list)
                     (map osgi/bundle->map))
        deps (get-deps bundles)]
    (dot/digraph
     [;(map #(-> [(:id %) {:label (:name %) :shape :box}]) bundles)
      (->> bundles
           (filter #(re-matches #"catalog-ui-search" (:name %)))
           (mapcat
            (fn [bundle]
              (map #(-> [(:name bundle) (get deps (first %))])
                   (get-in bundle [:headers :import-package])))))])))

(defn service-deps []
  (let [bundles (map osgi/bundle->map (take-last 10 (osgi/bundle-list)))]
    (dot/digraph
     [(map #(-> [(:id %) {:label (:name %) :shape :box}]) bundles)])))

(comment
  (clojure.pprint/pprint (map :location (map osgi/bundle->map (osgi/bundle-list))))
  (render (package-deps))
  (render (service-deps)))
