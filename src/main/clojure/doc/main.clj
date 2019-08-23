(ns doc.main
  (:require clojure.pprint
            [clojure.xml :as xml]
            [codox.main :refer [generate-docs]]))

(defn project-info []
  (->>
    (:content (xml/parse "pom.xml"))
    (filter #(#{:artifactId :groupId :version :description} (:tag %)))
    (map #(-> [(:tag %) (first (:content %))]))
    (into {})))

(defn -main []
  (let [{:keys [artifactId groupId version description]} (project-info)]
    (generate-docs
      {:doc-files ["README.md"]
       :source-paths ["src/main/clojure"]
       :name artifactId
       :package (symbol groupId artifactId)
       :version version
       :description description
       :source-uri (str "https://github.com/djblue/" artifactId "/blob/master/{filepath}#L{line}")})))

(-main)
