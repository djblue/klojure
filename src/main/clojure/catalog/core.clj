(ns catalog.core
  (:require [osgi.core :as osgi])
  (:import (org.apache.shiro.util ThreadContext)
           (ddf.catalog.data Metacard AttributeDescriptor MetacardType AttributeType$AttributeFormat)
           (klojure KlojureMetacard)
           (ddf.catalog.operation QueryRequest Query)
           (org.codice.ddf.security.common Security)
           (ddf.catalog.operation.impl QueryRequestImpl QueryImpl CreateRequestImpl UpdateRequestImpl DeleteRequestImpl)
           (java.util Set Date List)
           (ddf.catalog.data.impl MetacardTypeImpl MetacardImpl AttributeDescriptorImpl BasicTypes)
           (ddf.catalog CatalogFramework)
           (org.opengis.filter Filter)
           (ddf.catalog.filter FilterBuilder)
           (clojure.lang PersistentVector PersistentArrayMap)))

(defn metacard-attr [^Metacard metacard]
  (fn [^AttributeDescriptor descriptor]
    (let [name (.getName descriptor)
          attr (.getAttribute metacard name)]
      (if (nil? attr)
        nil
        [(keyword "catalog.core" name)
         (if (.isMultiValued descriptor)
           (.getValues attr)
           (.getValue attr))]))))

(defn metacard->map [^Metacard metacard]
  (let [metacard-type (.getMetacardType metacard)]
    (with-meta
      (->> metacard-type
           .getAttributeDescriptors
           (map (metacard-attr metacard))
           (filter not-empty)
           (into {}))
      {:metacard-type metacard-type})))

(def types {String    BasicTypes/STRING_TYPE
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

(defn attribute-descriptor ^AttributeDescriptor [[k v]]
  (let [multi? (instance? List v)]
    (AttributeDescriptorImpl.
     (name k)
     false
     true
     false
     multi?
     (get types
          (type ((if multi? first identity) v))))))

(defn map->metacard ^Metacard [m]
  (let [^Set descriptors (set (map attribute-descriptor m))
        metacard-type (MetacardTypeImpl. "test" descriptors)
        impl (MetacardImpl. metacard-type)]
    (doseq [[k v] m]
      (.setAttribute impl (name k) v))
    impl))

(defn bind-admin []
  (ThreadContext/bind
   (.getGuestSubject (Security/getInstance) "guest")))

(defn get-catalog-framework ^CatalogFramework []
  (first (osgi/get-services "ddf.catalog.CatalogFramework")))

(defn get-filter-builder ^FilterBuilder []
  (first (osgi/get-services "ddf.catalog.filter.FilterBuilder")))

(defn vector->filter ^Filter
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

(defn query-response->map [response]
  (->> (.getResults response)
       (map #(KlojureMetacard. (.getMetacard %)))))

(defmulti query class)

(defmethod query Filter [filter]
  (bind-admin)
  (query-response->map
   (.query (get-catalog-framework)
           (QueryRequestImpl. (QueryImpl. filter) false))))

(defmethod query PersistentVector [v]
  (query (vector->filter v)))

(defmulti create! (fn [a & _] (class a)))

(defn create-response->map [response]
  (->> (.getCreatedMetacards response)
       (map metacard->map)))

(defmethod create! Metacard [& metacards]
  (bind-admin)
  (create-response->map
   (.create (get-catalog-framework)
            (CreateRequestImpl. metacards))))

(defmethod create! PersistentArrayMap [& metacards]
  (->> metacards
       (map map->metacard)
       (apply create!)))

(defn update! [^String id ^Metacard metacard]
  (bind-admin)
  (.update (get-catalog-framework)
           (UpdateRequestImpl. id metacard)))

(defn delete! [& ids]
  (bind-admin)
  (.delete (get-catalog-framework)
           (DeleteRequestImpl. (into-array String ids))))

(comment
  (require 'clojure.rep)

  (query [[:attribute Metacard/ID] :is :like [:text "*"]])

  (create! {:id "" :title "one"}
           {:id "" :title "two"})

  (->> (query [[:attribute Metacard/ID] :is :like [:text "*"]])
       (map ::id)
       (apply delete!))

  (delete! "afa5099ccfcb40c3b39bade0148fc342")

  (query [[:attribute Metacard/TAGS] :is :like [:text "workspace"]]))
