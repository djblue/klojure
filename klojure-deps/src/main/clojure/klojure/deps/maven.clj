(ns klojure.deps.maven
  "Namespace that supports building the full transitive dependency graph of a Maven
  project, including subprojects, and provides query and manipulation tooling."
  (:require [clojure.java.shell :as shell]
            [clojure.xml :as xml])
  (:import (java.nio.file Paths Files)
           (java.nio.file.attribute FileAttribute)))

;; ----------------------------------------------------------------------
;; # REPL Constants
;;
;; As desired or necessary, change these to point to specific locations on
;; your machine.

(def ddf-home (System/getProperty "ddf.home"))
(defn- validate-exists [dir-str]
  (if (nil? ddf-home)
    dir-str
    (do (-> dir-str
            (Paths/get (make-array String 0))
            (Files/createDirectories (make-array FileAttribute 0)))
        dir-str)))

;; Writes dep data to DDF_HOME/graphs/maven by default, so dirs should exist
;; when the REPL is loaded
(def my-output-dir (validate-exists (str ddf-home "/graphs/maven")))

;; May require changing to point at DDF source code
(def my-path-to-ddf (str ddf-home "/../../repos/codice/ddf"))

;; ----------------------------------------------------------------------
;; # Maven Shell Private Helpers
;;
;; Functions for invoking Maven to generate GraphML data.

(defn- mvn-get-all-valid-subprojs
  "Given a string path to a root Maven project, returns a seq of absolute, fully
  qualified path strings that point to child Maven projects where shell commands
  can be invoked."
  [root-project-path]
  (->> root-project-path
       clojure.java.io/file
       file-seq
       (filter
         (fn [file]
           (let [path (.toPath file)]
             (and
               (.isDirectory file)
               (.exists (.toFile (.resolve path "pom.xml")))
               (.exists (.toFile (.resolve path "src")))
               (not (.contains (.getParent file) "/target"))))))
       (map #(let [p (.getPath %)]
               (do (println (str "Found " p)) p)))))

;; cp: repos/codice/ddf/libs/libs-pomfix/test/resources/features/target/tree.xml:
;; No such file or directory
(comment
  (mvn-get-all-valid-subprojs my-path-to-ddf)
  (clojure.pprint/pprint (take 10 (file-seq (clojure.java.io/file my-path-to-ddf)))))

(defn- mvn-save-deps
  "Given a string path to some Maven project, write the dependency tree to the
  target directory in GraphML format."
  [path]
  (let [result
        (shell/sh
          "mvn" "dependency:tree" "-nsu" "-DoutputType=graphml" "-DoutputFile=target/tree.xml"
          :dir path)]
    (do (println result) result)))

(comment
  (mvn-save-deps (str my-path-to-ddf "/platform/security/platform-security-core-api")))

(defn- generate-mvn-name
  "Not all Maven child projects have good, fully-qualified names. Given a string path to some
  child Maven project, generates a decent name, walking up the parent hierarchy if necessary."
  [path]
  (let [dir (clojure.java.io/file path)
        name (.getName dir)]
    (if (.contains name "-")
      name
      (loop [next-dir dir
             mvn-name name]
        (let [par-dir (.getParentFile next-dir)
              par-name (.getName par-dir)]
          (if (= "ddf" par-name)
            mvn-name
            (recur par-dir (str par-name "_" mvn-name))))))))

(comment
  (generate-mvn-name (str my-path-to-ddf "/platform/security/platform-security-core-api"))
  (generate-mvn-name (str my-path-to-ddf "/platform/parser/xml")))

(defn- mvn-move-deps-file
  "Given a string path to some Maven project and a string path to some destination folder,
  copies the target/tree.xml file to the destination folder, renamed to match the Maven project."
  [path dest]
  (let [mvn-name (generate-mvn-name path)
        result (shell/sh "cp" (str path "/target/tree.xml") (str dest "/" mvn-name ".xml"))]
    (do (println result) result)))

(comment
  (mvn-move-deps-file
    (str my-path-to-ddf "/platform/security/platform-security-core-api")
    my-output-dir)
  (mvn-move-deps-file
    (str my-path-to-ddf "/platform/parser/xml")
    my-output-dir))

;; ----------------------------------------------------------------------
;; # GraphML Private Helpers :: GraphML & XML
;;
;; Functions for manipulating GraphML data as XML.

(defn- graphml-parse
  "Parses the file at the given string path."
  [path]
  (let [file (clojure.java.io/file path)]
    (if-not (.exists file)
      (throw (IllegalArgumentException. (str "Path does not exist: " path)))
      (xml/parse file))))

(comment
  (graphml-parse (str ddf-home "/graphs/maven/blahblahblah.xml"))
  (graphml-parse (str ddf-home "/graphs/maven/platform-security-core-api.xml")))

(defn- graphml-remove-root
  "Removes the (for now) unnecessary root elements and returns only the XML
  representing the list of nodes and edges."
  [graphml]
  (->> graphml
       :content
       (filter #(= :graph (:tag %)))
       first
       :content))

(defn- graphml-dfs-for-content
  "Performs a depth-first search for the first string content item. Not actually
  specific to the GraphML schema, but XML in general."
  [xml]
  (if-not (contains? xml :content)
    (throw (IllegalArgumentException. (str "Invalid XML element: " xml)))
    (loop [xml-node xml]
      (let [next (first (:content xml-node))]
        (if (instance? String next)
          next
          (if-not (contains? next :content)
            (throw (IllegalArgumentException. (str "Invalid XML element: " next)))
            (recur next)))))))

(comment
  (graphml-dfs-for-content
    {:tag     :edge,
     :attrs   {:target "1843324431", :source "1267082248"},
     :content [{:tag     :data,
                :attrs   {:key "d1"},
                :content [{:tag     :y:PolyLineEdge,
                           :attrs   nil,
                           :content [{:tag :y:EdgeLabel, :attrs nil, :content ["compile"]}]}]}]})
  (graphml-dfs-for-content
    {:tag     :node,
     :attrs   {:id "1843324431"},
     :content [{:tag     :data,
                :attrs   {:key "d0"},
                :content [{:tag     :y:ShapeNode,
                           :attrs   nil,
                           :content [{:tag     :y:NodeLabel,
                                      :attrs   nil,
                                      :content ["joda-time:joda-time:jar:2.10:compile"]}]}]}]}))

;; ----------------------------------------------------------------------
;; # GraphML Private Helpers :: Clojure Normalized Form
;;
;; Functions for transforming GraphML data into Clojure maps.

(defn- graphml-normalize-node
  "Transforms a GraphML node into a simple map."
  [node]
  (if-not (= :node (:tag node))
    (throw (IllegalArgumentException. (str "Graph element was not a node: " node)))
    {:id   (:id (:attrs node))
     :name (graphml-dfs-for-content node)}))

(comment
  (graphml-normalize-node
    {:tag     :node,
     :attrs   {:id "1843324431"},
     :content [{:tag     :data,
                :attrs   {:key "d0"},
                :content [{:tag     :y:ShapeNode,
                           :attrs   nil,
                           :content [{:tag     :y:NodeLabel,
                                      :attrs   nil,
                                      :content ["joda-time:joda-time:jar:2.10:compile"]}]}]}]}))

(defn- graphml-normalize-edge
  "Transforms a GraphML edge into a simple map."
  [edge]
  (if-not (= :edge (:tag edge))
    (throw (IllegalArgumentException. (str "Graph element was not an edge: " edge)))
    {:target (:target (:attrs edge))
     :source (:source (:attrs edge))
     :name   (graphml-dfs-for-content edge)}))

(comment
  (graphml-normalize-edge
    {:tag     :edge,
     :attrs   {:target "1843324431", :source "1267082248"},
     :content [{:tag     :data,
                :attrs   {:key "d1"},
                :content [{:tag     :y:PolyLineEdge,
                           :attrs   nil,
                           :content [{:tag :y:EdgeLabel, :attrs nil, :content ["compile"]}]}]}]}))

(defn- graphml-normalize
  "Meant for use on a collection of GraphML XML nodes that represent graph nodes and
  graph edges, transforming them from XML S-Expressions to simple map representations."
  [xml]
  (let
    [normalize-any
     (fn [graph-xml-obj]
       (let [type (:tag graph-xml-obj)]
         (if-not (or (= type :node) (= type :edge))
           (throw (IllegalArgumentException.
                    (str "Unrecognized type for graph object: " graph-xml-obj)))
           (if (= type :node)
             (graphml-normalize-node graph-xml-obj)
             (graphml-normalize-edge graph-xml-obj)))))]
    (map normalize-any xml)))

(defn- graphml-link-edges
  "Takes the simple map representations of GraphML XML nodes and updates the graph
   edges to use Maven artifact names, not the GraphML-generated IDs. The graph nodes
   are lost during this process."
  [graph-objs]
  (let
    [
     ;; ----------------------------------------------------------------------
     ;; Need validation for simple node form
     node?
     (fn [obj]
       (and (contains? obj :id)
            (contains? obj :name)))
     ;; ----------------------------------------------------------------------
     ;; Need validation for simple edge form
     edge?
     (fn [obj]
       (and (contains? obj :target)
            (contains? obj :source)
            (contains? obj :name)))
     ;; ----------------------------------------------------------------------
     ;; Need to separate the nodes from the edges, and represent the nodes as
     ;; a map from ID to artifact name.
     separation
     (reduce
       (fn [out in]
         (if-not (or (node? in) (edge? in))
           (throw (IllegalArgumentException.
                    (str "Missing keys on graph component: " in)))
           (if (node? in)
             (assoc out :nodes (assoc (:nodes out) (:id in) (:name in)))
             (assoc out :edges (conj (:edges out) in)))))
       {:edges [] :nodes {}} graph-objs)
     ;; ----------------------------------------------------------------------
     ;; Above is just prep, the following values are the critical ones
     id-node-map (:nodes separation)
     edges (:edges separation)]
    ;; ----------------------------------------------------------------------
    ;; Iterate along the edge maps and perform ID substitution
    (map (fn [e] {:target (get id-node-map (:target e))
                  :source (get id-node-map (:source e))
                  :name   (:name e)})
         edges)))

(defn- graphml-format-node-names
  "Formats the graph node names for consistency."
  [edges]
  (let [format-name
        ;; Format for the label strings that Maven generated, helps document the split indicies
        ;; groupId:artifactId:packaging:version:scope
        #(let [parts (.split % ":")] (str (get parts 0) "/" (get parts 1) "/" (get parts 3)))
        format-map
        #(-> % (assoc :target (format-name (:target %))) (assoc :source (format-name (:source %))))]
    (map format-map edges)))

(comment
  (graphml-format-node-names
    [{:target "org.apache.logging.log4j:log4j-api:jar:2.8.2:compile",
      :source "ddf.security.core:security-core-api:bundle:2.17.2",
      :name   "compile"}]))

;; ----------------------------------------------------------------------
;; # Tools
;;
;; Top-level invokable functions for the REPL.

(defn mvn-generate-deps-for-proj
  "Given a string path to some Maven project, "
  [path]
  (->> path
       mvn-get-all-valid-subprojs
       (pmap mvn-save-deps)
       (filter #(not= 0 (:exit %)))))

(comment
  (mvn-generate-deps-for-proj my-path-to-ddf))

(defn mvn-move-deps-to
  "Given a string path to some Maven project and a string path to some destination folder,
  aggregate, rename, and move all tree.xml files to the destination."
  [path dest]
  (let [move-to-dest (fn [p] (mvn-move-deps-file p dest))]
    (->> path
         mvn-get-all-valid-subprojs
         (pmap move-to-dest)
         (filter #(not= 0 (:exit %))))))

(comment
  (mvn-move-deps-to my-path-to-ddf my-output-dir))

(defn mvn-list-dep-files
  "Given a string path to any directory, returns a list of fully qualified path strings
  for all xml files in that directory."
  [path]
  (->> path
       clojure.java.io/file
       file-seq
       (map #(.getPath %))
       (filter #(.endsWith % ".xml"))))

(comment
  (mvn-list-dep-files my-output-dir))

(defn mvn-edges-as-adjacency-list
  "Transforms edge maps into an adjacency list (vector of vectors)."
  [edges]
  ;; Return a distinct coll since the :name information is gone
  (->> edges
       (map #(vector (:source %) (:target %)))
       distinct
       vec))

(comment
  (mvn-edges-as-adjacency-list
    [{:target "commons-lang/commons-lang/2.6"
      :source "ddf.security.core/security-core-api/2.17.2"
      :name   "compile"}]))

(defn mvn-list-edges
  "Given a string path to a graphml file, extract edge information. Sample edge map:
  {:target  commons-lang/commons-lang/2.6
   :source  ddf.security.core/security-core-api/2.17.2
   :name    compile }"
  [path]
  (->> path
       graphml-parse
       graphml-remove-root
       graphml-normalize
       graphml-link-edges
       graphml-format-node-names))

(comment
  (->> (str ddf-home "/graphs/maven/platform-security-core-api.xml")
       mvn-list-edges
       #_(filter #(= (:name %) "test"))
       mvn-edges-as-adjacency-list))

(defn mvn-collect-edges
  "Given a collection of paths, aggregate all edges across all maven artifacts
  across all graphml files included in the collection."
  [paths]
  (->> paths
       (map graphml-parse)
       (map graphml-remove-root)
       (map graphml-normalize)
       (map graphml-link-edges)
       (map graphml-format-node-names)
       flatten
       distinct))

(comment
  ;; Snippets and functions useful for understanding the data
  (count)
  (distinct)
  (filter (fn [[k v]] (> v 1)))
  (frequencies)

  ;; Comprehensive edge map list for specified dependency files
  (->> [(str ddf-home "/graphs/maven/platform-security-core-api.xml")
        (str ddf-home "/graphs/maven/platform-security-encryption-api.xml")
        (str ddf-home "/graphs/maven/platform-security-encryption-commands.xml")
        (str ddf-home "/graphs/maven/platform-security-encryption-crypter.xml")
        (str ddf-home "/graphs/maven/platform-security-encryption-impl.xml")]
       mvn-collect-edges)

  ;; Comprehensive edge map list for security
  (->> my-output-dir
       mvn-list-dep-files
       (filter #(or (.contains % "/security-") (.contains % "/platform-security-")))
       mvn-collect-edges)

  ;; Full compile/provided scope dependency graph for security
  (->> my-output-dir
       mvn-list-dep-files
       (filter #(or (.contains % "/security-") (.contains % "/platform-security-")))
       mvn-collect-edges
       (filter #(not= (:name %) "test"))
       mvn-edges-as-adjacency-list)

  ;; Full compile/provided scope dependency graph for all of DDF
  (->> my-output-dir
       mvn-list-dep-files
       mvn-collect-edges
       (filter #(not= (:name %) "test"))
       mvn-edges-as-adjacency-list))