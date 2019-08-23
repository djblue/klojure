(ns deps.common-test
  (:require [deps.common :as c]
            [clojure.test :refer [deftest is]]))

(deftest test-collmap-inverted
  (is (= (c/collmap-inverted {:a [1 2] :b [2 3]})
         {1 :a, 2 :b, 3 :b})))
