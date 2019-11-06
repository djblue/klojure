(ns catalog.specs
  (:refer-clojure :exclude [or])
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:import (java.util Date)))

(defn date? [a] (instance? Date a))

(defn gen-date []
  (gen/fmap #(Date. (* % 1000)) (s/gen pos-int?)))

(defmacro defterm [k & vs]
  `(s/def ~k (s/cat :key #{~(-> k name keyword)} ~@(interleave [:v1 :v2 :v3] vs))))

(defmacro or [& args]
  `(s/or ~@(mapcat #(-> [(-> % name keyword) %]) args)))

(defmacro alt [& args]
  `(s/alt ~@(mapcat #(-> [(-> % name keyword) %]) args)))

(defmacro op [op arg]
  `(s/cat :op ~op :arg ~arg))

(defterm ::number number?)
(defterm ::numbers number? number?)
(defterm ::wkt string?)
(defterm ::date (s/with-gen date? gen-date))
(defterm ::date-range
         (s/with-gen date? gen-date)
         (s/with-gen date? gen-date))
(defterm ::bool boolean?)
(defterm ::bytes bytes?)
(defterm ::empty)
(defterm ::text string?)
(defterm ::fuzzy string?)
(defterm ::case-sensitive-text string?)

(s/def ::terminal
  (or ::number
      ::numbers
      ::wkt
      ::date
      ::date-range
      ::bool
      ::bytes
      ::empty
      ::text
      ::fuzzy
      ::case-sensitive-text))

(s/def ::inequality
  (op #{:< :less-than :lessThan
        :<= :less-than-or-equal-to :lessThanOrEqualTo
        :> :greater-than :greaterThan
        :>= :greater-than-or-equal-to :greaterThanOrEqualTo}
      (or ::number)))

(s/def ::equality
  (op #{:= :equal-to :equalTo} (or ::terminal)))

(s/def ::like
  (op #{:like} (or ::text ::fuzzy ::case-sensitive-text)))

(s/def ::temporal
  (op #{:after :before} (or ::date)))

(s/def ::temporal-range
  (op #{:during :overlapping} (or ::date-range)))

(s/def ::expression
  (alt ::inequality
       ::equality
       ::like
       ::empty
       ::temporal
       ::temporal-range))

(s/def ::keys #{"id" "metacard"})

(s/def ::attribute-pair
  (s/cat :key #{:attribute} :value ::keys))

(s/def ::attribute
  (s/cat
    :attribute (or ::attribute-pair)
    :is #{:is}
    :expression ::expression))

(s/def ::xpath (s/cat :key #{:xpath} :value string?))
(s/def ::all-of (s/cat :key #{:all-of :allOf} :value (s/+ ::filter)))
(s/def ::any-of (s/cat :key #{:any-of :anyOf} :value (s/+ ::filter)))
(s/def ::not (s/cat :key #{:not} :value ::filter))

(s/def ::filter
  (or ::attribute
       ;::xpath
       ::all-of
       ::any-of
       ::not))

(comment
  (binding [s/*recursion-limit* 0]
    (gen/generate (s/gen ::filter))))
