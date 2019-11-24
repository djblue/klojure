(ns klojure.deps.artifacts
  "Namespace that supports queries against artifacts in the current
  user's .m2 directory and any pom XML files within."
  (:require [klojure.osgi.core :as osgi]
            [clojure.string :as s]
            [clojure.java.io :as io]))

(defn- user-home []
  (System/getProperty "user.home"))

(defn size-as-mb
  "Given a number representing bytes, returns the equivalent number
  of megabytes."
  [bytes]
  (/ bytes 1e+6))

(comment "Sample invocation." (size-as-mb 1000000))

(defn artifact-size
  "Given a maven coordinate, returns the size of the artifact in megabytes."
  [mvn-coord]
  (let [group (:g mvn-coord)
        artifact-id (:a mvn-coord)
        version (:v mvn-coord)
        path (str (user-home) "/.m2/repository/"
                  (s/replace group "." "/")
                  "/" artifact-id "/" version "/" artifact-id "-" version ".jar")
        file (io/file path)]
    (size-as-mb (.length file))))

(comment
  "Sample invocation."
  (artifact-size {:g "ddf.platform.util",
                  :a "platform-util",
                  :v "2.16.0-SNAPSHOT"}))

(defn artifacts-embedded
  "Returns a seq of maps representing JARs."
  []
  (->> (osgi/bundles)
       (map (fn [bundle] (get-in bundle [:headers :embedded-artifacts])))
       (mapcat vals)
       (frequencies)
       (map (fn [[artifact count]]
              (let [size (artifact-size artifact)]
                {:artifact artifact
                 :count count
                 :size size
                 :total-size (* size count)})))))

(comment
  "Sample invocation."
  (artifacts-embedded)
  "Sample map representing a JAR."
  {:artifact {:g "org.apache.httpcomponents",
              :a "httpclient",
              :v "4.5.6"},
   :count 18,
   :size 0.76714,
   :total-size 13.808520000000001})

(comment "How many total artifacts does DDF embed?"
  (->> (artifacts-embedded)
       (map :count)
       (reduce +)))

(comment "What are DDF's most embedded artifacts?"
  (->> (artifacts-embedded)
       (sort-by :count >)
       (take 10)))

(comment "How much of DDF's size comes from embedding?"
  (->> (artifacts-embedded)
       (map :total-size)
       (reduce +)))

(comment "Which artifacts most impact DDF's total embed cost?"
  (->> (artifacts-embedded)
       (sort-by :total-size >)
       (take 10)))