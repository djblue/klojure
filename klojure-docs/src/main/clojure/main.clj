(ns main
  (:require clojure.pprint
            [clojure.xml :as xml]
            [codox.main :refer [generate-docs]]))

(def sep (apply str (take 100 (repeat "-"))))

(defn- shout!
  ([data]
   (do (println sep) (println sep) (println data) data))
  ([msg data]
   (do (println sep) (println sep) (println msg) (println data) data)))

(defn- get-source-paths []
  (->> ["../klojure-catalog" "../klojure-deps"]
       (map #(str % "/src/main/clojure"))
       vec
       shout!))

(defn- project-info []
  (->>
    ;; Need to verify what mvn sets as working dir when building sub-modules
    (:content (xml/parse "../pom.xml"))
    (shout! (System/getProperty "user.dir"))
    (filter #(#{:artifactId :groupId :version :description} (:tag %)))
    (map #(-> [(:tag %) (first (:content %))]))
    (into {})
    shout!))

(defn -main []
  (let [{:keys [artifactId groupId version description]} (project-info)]
    (generate-docs)
    {:doc-files ["../README.md"]}
    :source-paths (get-source-paths)
    ;:source-paths [""]
    :name artifactId
    :package (symbol groupId artifactId)
    :version version
    :description description
    :namespaces [#"^(?!(doc|klojure)).*"]
    :metadata {:doc/format :markdown}
    :source-uri (str "https://github.com/djblue/" artifactId "/blob/master/{filepath}#L{line}")))

(-main)