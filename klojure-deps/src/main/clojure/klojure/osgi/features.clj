(ns klojure.osgi.features
  "WIP"
  (:require [klojure.osgi.core :as osgi]))

; May remove all of these if the bean mapping strategy works well enough
(defn feature->map [f]
  {:id            (.getId f)
   :name          (.getName f)
   :description   (.getDescription f)
   :details       (.getDetails f)
   :version       (.getVersion f)
   :resolver      (.getResolver f)
   :install       (.getInstall f)
   :isHidden      (.isHidden f)
   :dependencies  (map (fn [d] {:name         (.getName d)
                                :version      (.getVersion d)
                                :isPrereq     (.isPrerequisite d)
                                :isDependency (.isDependency d)})
                       (.getDependencies f))
   :bundles       (map (fn [b] {:location         (.getLocation b)
                                :originalLocation (.getOriginalLocation b)
                                :startLevel       (.getStartLevel b)
                                :isStart          (.isStart b)
                                :isDependency     (.isDependency b)})
                       (.getBundles f))
   :startLevel    (.getStartLevel f)
   :namespace     (.getNamespace f)
   :resourceRepos (.getResourceRepositories f)
   :repoUrl       (.getRepositoryUrl f)})

(defn features []
  (->> (osgi/list-features)
       (map feature->map)))

(comment (features))