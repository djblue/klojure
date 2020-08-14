(ns catalog.core
  "Namespace for interacting with DDF's Catalog Framework."
  (:require [osgi.core :as osgi])
  (:import (org.apache.shiro.util ThreadContext)
           (ddf.catalog.data Metacard AttributeDescriptor)
           (klojure KlojureMetacard)
           (org.codice.ddf.security Security)
           (ddf.catalog.operation.impl QueryRequestImpl QueryImpl CreateRequestImpl UpdateRequestImpl DeleteRequestImpl)
           (java.util Set Date List)
           (ddf.catalog.data.impl MetacardTypeImpl MetacardImpl AttributeDescriptorImpl BasicTypes)
           (ddf.catalog CatalogFramework)
           (org.opengis.filter Filter)
           (ddf.catalog.filter FilterBuilder)
           (clojure.lang PersistentArrayMap)
           (java.security PrivilegedAction)))

(defn- metacard-attr [^Metacard metacard]
  (fn [^AttributeDescriptor descriptor]
    (let [name (.getName descriptor)
          attr (.getAttribute metacard name)]
      (if (nil? attr)
        nil
        [(keyword "catalog.core" name)
         (if (.isMultiValued descriptor)
           (.getValues attr)
           (.getValue attr))]))))

(defn- metacard->map [^Metacard metacard]
  (let [metacard-type (.getMetacardType metacard)]
    (with-meta
      (->> metacard-type
           .getAttributeDescriptors
           (map (metacard-attr metacard))
           (filter not-empty)
           (into {}))
      {:metacard-type metacard-type})))

(def ^:private types
  {String    BasicTypes/STRING_TYPE
   Boolean   BasicTypes/BOOLEAN_TYPE
   Date      BasicTypes/DATE_TYPE
   Short     BasicTypes/SHORT_TYPE
   Integer   BasicTypes/INTEGER_TYPE
   Long      BasicTypes/LONG_TYPE
   Float     BasicTypes/FLOAT_TYPE
   Double    BasicTypes/DOUBLE_TYPE
   :geometry BasicTypes/GEO_TYPE
   bytes     BasicTypes/BINARY_TYPE
   :xml      BasicTypes/XML_TYPE
   :object   BasicTypes/OBJECT_TYPE})

(defn- attribute-descriptor ^AttributeDescriptor [[k v]]
  (let [multi? (instance? List v)]
    (AttributeDescriptorImpl.
     (name k)
     false
     true
     false
     multi?
     (get types
          (type ((if multi? first identity) v))))))

(defn- map->metacard ^Metacard [m]
  (let [^Set descriptors (set (map attribute-descriptor m))
        metacard-type (MetacardTypeImpl. "test" descriptors)
        impl (MetacardImpl. metacard-type)]
    (doseq [[k v] m]
      (.setAttribute impl (name k) v))
    impl))

(defn- get-security-service ^Security []
  (first (osgi/get-services "org.codice.ddf.security.Security")))

(defn- get-catalog-framework ^CatalogFramework []
  (first (osgi/get-services "ddf.catalog.CatalogFramework")))

(defn- get-filter-builder ^FilterBuilder []
  (first (osgi/get-services "ddf.catalog.filter.FilterBuilder")))

(defn- vector->filter ^Filter
  ([v] (vector->filter (get-filter-builder) v))
  ([b v]
   (loop [acc b
          [op & [a b :as operands]] v]
     (cond
       (nil? op) acc
       (vector? op) (recur (vector->filter acc op) operands)
       :else
       (case op
         ; terminal
         :attribute (.attribute acc a)
         :xpath (.xpath acc a)
         :text (.text acc a)
         :number (.number acc a)
         :numbers (.numbers acc a b)
         :wkt (.wkt acc a)
         :date (.date acc a)
         :date-range (.dateRange acc a b)
         :bool (.bool acc a)
         :bytes (.bytes acc a)
         :empty (.empty acc)

         ; recursive
         :not (.not acc (vector->filter a))
         (:any-of :anyOf) (.anyOf acc (map vector->filter operands))
         (:allOf :all-of) (.allOf acc (map vector->filter operands))

         ; iterative
         (recur
          (case op
            :is (.is acc)
            :like (.like acc)
            (:less-than :<) (.lessThan acc)
            (:less-than-or-equal-to :<=) (.lessThanOrEqualTo acc)
            (:greater-than :>) (.greaterThan acc)
            (:greater-than-or-equal-to :>=) (.greaterThanOrEqualTo acc)
            :between (.between acc)
            (:equal-to :=) (.equalTo acc)
            (:not-equal-to :!=) (.notEqualTo acc)
            :after (.after acc)
            :before (.before acc)
            :during (.during acc)
            :overlapping (.overlapping acc)
            :beyond (.beyond acc)
            :within-buffer (.withinBuffer acc)
            :intersecting (.intersecting acc)
            :containing (.containing acc)
            :within (.within acc)
            :nearest-to (.nearestTo acc))
          operands))))))

(defn get-source-ids []
  (.. (get-catalog-framework) (getSourceIds)))

(defn- query-response->map [response]
  (->> (.getResults response)
       (map #(KlojureMetacard. (.getMetacard %)))))

(defmacro ^:private as-admin [& body]
  `(.runAsAdmin (get-security-service)
     (reify PrivilegedAction
       (run [this]
         (ThreadContext/bind
           (.getSystemSubject (get-security-service)))
         ~@body))))

(defn query
  "Query the catalog framework.

  Example querying for all metacard:

      (query [[:attribute Metacard/ID] :is :like [:text \"*\"]])

  Example querying for all workspaces:

      (query [[:attribute Metacard/TAGS] :is :like [:text \"workspace\"]])
  "
  [filter]
  (as-admin
    (query-response->map
      (.query (get-catalog-framework)
              (QueryRequestImpl. (QueryImpl. (vector->filter filter)) false)))))

(defmulti create! (fn [a & _] (class a)))

(defn- create-response->map [response]
  (->> (.getCreatedMetacards response)
       (map metacard->map)))

(defmethod create! Metacard [& metacards]
  (as-admin
    (create-response->map
     (.create (get-catalog-framework)
              (CreateRequestImpl. metacards)))))

(defmethod create! PersistentArrayMap [& metacards]
  (->> metacards
       (map map->metacard)
       (apply create!)))

(defn update! [^String id ^Metacard metacard]
  (as-admin
    (.update (get-catalog-framework)
             (UpdateRequestImpl. id metacard))))

(defn delete! [& ids]
  (as-admin
    (.delete (get-catalog-framework)
             (DeleteRequestImpl. (into-array String ids)))))

(comment
  (require 'clojure.repl)

  (query [[:attribute Metacard/ID] :is :like [:text "*"]])

  (create! {:id "" :title "one"}
           {:id "" :title "two"})

  (->> (query [[:attribute Metacard/ID] :is :like [:text "*"]])
       (map ::id)
       (apply delete!))

  (delete! "afa5099ccfcb40c3b39bade0148fc342")

  (query [[:attribute Metacard/TAGS] :is :like [:text "workspace"]]))
