(ns klojure.deps.bundles
  "Namespace for manipulating core bundle data structures."
  (:require [klojure.osgi.core :as osgi]
            [klojure.deps.common :as c]))

;; ----------------------------------------------------------------------
;; # Predicates
;;
;; Useful functions for selection and filtering.

(defn select-on-all
  "Create a predicate that returns true if all functions in fns return true
  for the given input."
  [fns]
  (fn [x] (->> fns (map (fn [f] (f x))) (reduce (fn [a b] (and a b))))))

(defn select-bundles-built-by
  "Create a predicate for bundle defs that evals to true if a match is found
  against the name of the user who built the bundle."
  [user]
  (fn [b] (->> b (:headers) (:built-by) (= user))))

(defn select-bundles-by-name
  "Create a predicate for bundle defs that evals to true if a match is found
  against the bundle name, if it contains the provided text."
  [text]
  (fn [b] (.contains (:name b) text)))

(defn select-packages-ddf-only
  "Predicate for package strings that only evals to true when the package is a
  direct DDF package, not third party."
  []
  (fn [p] (or (.contains p "org.codice") (.contains p "ddf."))))

(comment
  ((select-packages-ddf-only) "org.codice.ddf.catalog.monitor")
  ((select-packages-ddf-only) "ddf.data.types")
  ((select-packages-ddf-only) "net.opensaml.pki"))

;; ----------------------------------------------------------------------
;; # Partitions
;;
;; Useful functions for grouping the bundle list.

(defn partition-bundles-by-id
  "Example function that partitions bundles. Takes a coll of bundle defs and returns
  a coll of bundle def colls."
  [bundles]
  (let [cushion 1
        id-set (set (map :id bundles))]
    (->> bundles
         (partition-by
           #(and
              (contains? id-set (+ (:id %) cushion))
              (contains? id-set (- (:id %) cushion)))))))

(comment
  (map :name (osgi/bundles))
  (->> (osgi/bundles)
       (filter (select-bundles-built-by "lambeaux"))
       (partition-bundles-by-id)
       (map #(map :name %))))

(defn- first-match
  "Returns the first substring in 'substrings' that is found within the given bundle's
  name. Returns nil if none are found."
  [substrings bundle]
  (let [bundle-name (:name bundle)]
    ; the 'some' function is just filter, then take first, a.k.a 'take-when'
    (some #(when (.contains bundle-name %) %) substrings)))

(comment
  (first-match
    ["security-", "platform-", "ctalog-"]
    {:name "platform-catalog-core-blah"}))

(defn partition-bundles-by-name
  "Partitions bundles into groups using matches against 'names' when compared to the
  bundle's name. All bundles that did not match any names are grouped together."
  [names bundles]
  (vals
    (reduce
      (fn [out in]
        (let [group-name (first-match names in)
              group (get out group-name)]
          (assoc out group-name (conj group in))))
      {}
      bundles)))

(comment
  "Taking a vector of strings and coll of bundles and reduce to a map"
  (partition-bundles-by-name
    ["cat-core-" "cat-" "sec-core-" "sec-"]
    [{:name "cat-core-a"}
     {:name "cat-core-b"}
     {:name "sec-core-a"}
     {:name "sec-core-b"}
     {:name "cat-io-a"}
     {:name "cat-io-b"}
     {:name "sec-pki-api"}
     {:name "bob"}]))

;; ----------------------------------------------------------------------
;; # Manifest
;;
;; Useful functions for working with bundles' manifest info.

(defn- bundle-extract-header-info
  "Returns an f that maps bundle names to header info, where k is the key in the headers
  to map to."
  [k]
  (fn [bundles]
    (->> bundles
         (map (fn [bundle]
                (let [name (:name bundle)]
                  {name (->> bundle
                             (:headers)
                             k
                             (map key))})))
         (into {}))))

(def bundle-imports (bundle-extract-header-info :import-package))
(def bundle-exports (bundle-extract-header-info :export-package))

(comment
  (bundle-imports (take 20 (osgi/bundles)))
  (bundle-exports (take 20 (osgi/bundles))))

(defn package-depmap
  "Turns a coll of bundle defs into an edge list of bundle dependencies using packages
  to define the links. Use f as the package filter."
  [f bundles]
  (let [imports (c/collmap-filtered f (bundle-imports bundles))
        exports (c/collmap-inverted (c/collmap-filtered f (bundle-exports bundles)))]
    (into {}
          (map
            (fn [[bundle package-imports]]
              [bundle (->> package-imports
                           (map #(get exports %))
                           (filter #(not (= nil %)))
                           (set))])
            imports))))

(comment
  (bundle-imports (take 30 (osgi/bundles)))
  (package-depmap (constantly true) (osgi/bundles))
  (package-depmap (select-packages-ddf-only) (osgi/bundles)))