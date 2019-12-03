(ns klojure.deps.common
  "Namespace for functions that serve as extensions to clojure/core. They primarily
  work with abstract data structures useful for specifying graphs and dependencies:
  - Collmaps: maps that have seq-able things or collections as their values.
  TODO: Consider renaming 'collmap' --> 'multimap' given it's a more widely used term.
  - Partitions: colls of colls, where the contents of a seq-able thing are themselves
                seq-able, so tools for flat mapping and merging are provided.
  ")

(defn merge-left
  "Merges the contents of vectors within a vector, where elements move left.
  Elements only move if they are alone in their inner vector. Empty vectors
  are completely dissolved.

  (merge-left [[1] [2] [3 4 5] [] [7] [8 9 10] [11] [12]])
  => [[1 2] [3 4 5 7] [8 9 10 11 12]]
  "
  [v]
  (reduce
    (fn [out in]
      (->> (conj out in)
           (if (= (count in) 1)
             (conj (pop out) (conj (last out) (first in))))
           (if (= (count in) 0)
             out)))
    [[]] v))

(comment
  (merge-left [[1] [2] [3 4 5] [6] [7] [8 9 10] [11] [12]])
  (merge-left [[1] [2] [3 4 5] [] [7] [8 9 10] [11] [12]]))

(defn collmap-inverted
  "Inverts a map whose values are colls.
  Turns a map whose values are colls into the reverse view where each value in
  each coll becomes a key mapped to its original key. For non-unique values in
  colls across different keys, the last one in wins.

  (collmap-inverted {:a [1 2] :b [2 3]})
  => {1 :a, 2 :b, 3 :b}
  "
  [m]
  (->> m
       (map (fn [[k vs]] (map vector vs (repeat k))))
       (apply concat)
       (into {})))

(comment
  (collmap-inverted {:a [1 2 3] :b [4 5 6]})
  (collmap-inverted {:a [1 2] :b [2 3]}))

(defn collmap-filtered
  "Returns a new map of key to new colls that have been filtered according to f.

  (collmap-filtered (fn [i] (> i 6)) {:a [1 2 7 8] :b [3 4 8 9]})
  => {:a (7 8), :b (8 9)}
  "
  [f m]
  (let [re-pair (fn [[k vs]] [k (filter f vs)])]
    (into {} (map re-pair m))))

(comment
  (collmap-filtered (constantly true) {:a [1 2 7 8] :b [3 4 8 9]})
  (collmap-filtered (fn [i] (> i 6)) {:a [1 2 7 8] :b [3 4 8 9]}))

(defn collmap-collapsed
  "Collapses a map whose values are colls.
  Turns a map whose values are colls into a coll of size-2 vectors representing
  the entire mapping, akin to a list of edges in a graph.

  (collmap-collapsed {:a [:x :y], :b [:s :t]})
  => ([:a :x] [:a :y] [:b :s] [:b :t])
  "
  [m]
  (mapcat
    (fn [[k v]]
      (map vector (repeat k) v))
    m))

(comment
  (collmap-collapsed {:a [:x :y], :b [:s :t]}))