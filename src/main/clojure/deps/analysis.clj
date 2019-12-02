(ns deps.analysis
  "Namespace for composing and rendering data and visualizations."
  (:require [osgi.core :as osgi]
            [deps.common :as c]
            [deps.maven :as m]
            [deps.bundles :as b]
            [clojure.java.shell :refer [sh]]
            [loom.alg :as lm-alg]
            [loom.derived :as lm-der]
            [loom.graph :as lm-gra]
            [loom.gen :as lm-gen]
            [loom.io :as lm-io]
            [dorothy.core :as dot]
            [dorothy.jvm :refer [save! show!]]
            [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.string :as string]
            [clojure.data.json :as json])
  (:import (java.io ByteArrayInputStream)))

(defn- user-name []
  (System/getProperty "user.name"))

(def bundle-pars
  ["spatial-"
   "catalog-ui"
   "catalog-validator-"
   "catalog-transformer-"
   "catalog-security-"
   "catalog-rest-"
   "catalog-plugin-"
   "catalog-opensearch-"
   "catalog-core-"
   "catalog-admin-"
   "catalog-"
   "security-sts-"
   "security-servlet-"
   "security-rest-"
   "security-idp-"
   "security-handler-"
   "security-"
   "session-"
   "platform-"
   "persistence-"
   "mime-"
   "metrics-"
   "admin-"
   "action-"
   "solr-"])

;; ----------------------------------------------------------------------
;; # Rendering functions / graph support
;;
;; Reference for writing graphviz graphs with dorothy and the tools for viewing them.

(def graph-properties
  {:formats [:png :svg]
   :layouts [:dot :neato :circo]})

(defn view-by-show
  "Draws the graph using Java Swing."
  [graph]
  (-> (dot/digraph graph)
      (dot/dot)
      (show! {:layout :neato})))

(defn view-after-save
  "Draws the graph by writing a file to /tmp before opening it with the system's
  default viewing software."
  ([graph]
   (view-after-save graph {:format :svg :layout :dot}))
  ([graph props]
   (let [file "/tmp/render.svg"]
     (-> graph dot/dot (save! file props))
     (sh "open" file))))

(defn layer-create-edge
  "Used for mapping bundle defs to graphviz data structures."
  [f bundles]
  (into [{}
         (dot/edge-attrs {:color :black})]
        (c/collmap-collapsed (b/package-depmap f bundles))))

(defn layer-create-node
  "Used for mapping bundle defs to graphviz data structures."
  [layer-name layer-label bundles]
  (dot/subgraph
    layer-name
    (into [{:color :blue :label layer-label}
           (dot/node-attrs {:style :filled})]
          (map vector bundles))))

(defn layer-bulkcreate-nodes
  "Used for mapping bundle defs to graphviz data structures."
  [bundles]
  (->> bundles
       (b/partition-bundles-by-id)
       (map-indexed
         (fn [idx bundles]
           (let [cluster (keyword (str "cluster_" idx))]
             (layer-create-node cluster (str "test_" idx) (map :name bundles)))))))

;; ----------------------------------------------------------------------
;; # HTML Output
;;
;; Exporting Loom graphs for an interactive HTML document.

(def ddf-home (System/getProperty "ddf.home"))
(def viz-file (str ddf-home "/graphs/viz.html"))
(def viz-resource "templates/graph-output.html")

(defn- write-html-file [json-string-nodes json-string-edges]
  (let [template (->> viz-resource io/resource slurp)
        processed (-> template
                      (clojure.string/replace #"\"REPLACE_NODES\"" json-string-nodes)
                      (clojure.string/replace #"\"REPLACE_EDGES\"" json-string-edges))]
    (spit viz-file processed :create true)))

(comment
  (write-html-file
    "[{\"id\": \"A\", \"label\": \"A\"},{\"id\": \"B\", \"label\": \"B\"}]"
    "[{\"from\": \"A\", \"to\": \"B\"}]"))

(defn- graph->node-json [graph]
  (->> graph
       (lm-gra/nodes)
       (map #(hash-map :id % :label %))
       vec
       clojure.data.json/write-str))

(comment
  (graph->node-json (lm-gra/digraph ["A" "B"] ["B" "D"] ["C" "D"] ["D" "E"])))

(defn- graph->edge-json [graph]
  (->> graph
       (lm-gra/edges)
       (map #(hash-map :from (get % 0) :to (get % 1)))
       vec
       clojure.data.json/write-str))

(comment
  (graph->edge-json (lm-gra/digraph ["A" "B"] ["B" "D"] ["C" "D"] ["D" "E"])))

(defn export-as-html
  ""
  [graph]
  (write-html-file
    (graph->node-json graph)
    (graph->edge-json graph)))

(comment
  ;; Generating random graphs for testing
  (lm-io/view
    (lm-gra/digraph ["A" "B"] ["B" "D"] ["C" "D"] ["D" "E"]))
  (lm-gen/gen-rand (lm-gra/digraph) 10 10)

  ;; Transform loom to JSON
  (export-as-html (lm-gra/digraph ["A" "B"] ["B" "D"] ["C" "D"] ["D" "E"]))

  ;; Manipulating the HTML document as XML
  (with-open [s (.openStream (io/resource "templates/graph-output.html"))]
    (xml/parse s))

  ;; Manipulating the HTML document as text (string substitution)
  (-> (->> "templates/graph-output.html" io/resource slurp)
      (clojure.string/replace #"\"REPLACE_NODES\"" "nodes")
      (clojure.string/replace #"\"REPLACE_EDGES\"" "edges"))

  {:id "" :label ""}                                        ;; node
  {:from "" :to ""}                                         ;; edge

  ())

;; ----------------------------------------------------------------------
;; # Loom Graph Support
;;
;; Composing dependency data into Loom graphs for better filtering capabilities.

(defn graph-maven-dependencies
  "Given a working directory and a map of filters, generate a graph that conforms to the filtering
  criteria. The map of filters only contains predicate functions and has the following keys:
  :select-modules, a function of a string path that determines if the graphml file it points to
                   should be included.
  :select-edges, a function of a maven edge that determines if it should be included."
  [working-dir filters]
  (->> working-dir
       m/mvn-list-dep-files
       (filter (:select-modules filters))
       m/mvn-collect-edges
       (filter (:select-edges filters))
       m/mvn-edges-as-adjacency-list
       (apply lm-gra/digraph)))

(defn subgraph-top-down
  "Given a loom graph and a particular node in the graph, return the subgraph of all reachable
  nodes from the given node, where traversal follows the direction of edges, useful for tracing
  high level dependencies.
  The node should be a string of the form groupId/artifactId/version."
  [graph node]
  (lm-der/subgraph-reachable-from graph node))

(defn subgraph-bottom-up
  "Given a loom graph and a particular node in the graph, return the subgraph of all reachable
  nodes from the given node, where traversal goes in the opposite direction of the edges, useful
  for tracing low level dependencies.
  The node should be a string of the form groupId/artifactId/version."
  [graph node]
  (-> graph
      lm-gra/transpose
      (lm-der/subgraph-reachable-from node)
      lm-gra/digraph
      lm-gra/transpose))

;; ----------------------------------------------------------------------
;; # Filters & Predicates
;;
;; Examples of commonly used filter functions for working with DDF data.

(def select-all (fn [x] (constantly true)))
(def select-security-modules #(or (.contains % "/security-") (.contains % "/platform-security-")))
(def select-catalog-modules #(or (.contains % "/catalog-") (.contains % "/catalog_")))
(def select-no-test-deps #(not= (:name %) "test"))

(def select-only-relevant-modules
  #(not
     (or (.contains % "/broker")
         (.contains % "/features")
         (.contains % "/sample")
         (.contains % "/sdk")
         (.contains % "/test"))))

(def select-only-relevant-edges
  #(let [src (:source %)]
     (not
       (or #_(.startsWith src "ddf.features")
         (.startsWith src "ddf.test")
         (.startsWith src "org.codice.ddf.broker")
         (.contains src "sample-")
         (.contains src "test-")))))

(def select-internal-edges
  #(let [tar (:target %)]
     (or (.startsWith tar "ddf")
         (.startsWith tar "org.codice")
         (.startsWith tar "com.connexta"))))

(def filter-default
  {:select-modules select-all
   :select-edges   (b/select-on-all
                     [select-no-test-deps
                      select-only-relevant-edges
                      select-internal-edges])})

(def filter-security
  {:select-modules select-security-modules
   :select-edges   (b/select-on-all [select-no-test-deps select-internal-edges])})

(def filter-security-include-third-party
  {:select-modules select-security-modules
   :select-edges   select-no-test-deps})

(def filter-catalog
  {:select-modules select-catalog-modules
   :select-edges   select-no-test-deps})

;; ----------------------------------------------------------------------
;; # Graphs & Subgraphs
;;
;; Examples of building subgraphs defined with respect to certain dependencies of interest.

(comment
  "Draw the full maven dependency graph for DDF, without test or third party deps, and output
  as HTML for interactive exploration."
  (->> (graph-maven-dependencies m/my-output-dir filter-default)
       (export-as-html)))

(comment
  "Draw the full maven dependency graph for security, without test or third party deps."
  (-> (graph-maven-dependencies m/my-output-dir filter-security)
      (lm-io/view :fmt :svg)))

(comment
  "Draw the subgraph of all security nodes that are dependencies of the IDP server"
  (-> (graph-maven-dependencies m/my-output-dir filter-security)
      (subgraph-top-down "ddf.security.idp/security-idp-server/2.17.2")
      (lm-io/view :fmt :svg)))

(comment
  "Draw the subgraph of all catalog nodes that transitively depend on catalog-core-api"
  (-> (graph-maven-dependencies m/my-output-dir filter-catalog)
      (subgraph-bottom-up "ddf.catalog.core/catalog-core-api/2.17.2")
      (lm-io/view :fmt :svg)))

(comment
  "Draw the subgraph of all security nodes that transitively depend on guava"
  (-> (graph-maven-dependencies m/my-output-dir filter-security-include-third-party)
      (subgraph-bottom-up "com.google.guava/guava/20.0")
      (lm-io/view :fmt :svg)))

;; ----------------------------------------------------------------------
;; # Metrics & Analysis
;;
;; Examples of sorting dependencies based upon their graph metrics.

(comment)

(comment
  "List nodes that no one depends on."
  (let [g (graph-maven-dependencies m/my-output-dir filter-default)]
    (->> (lm-gra/nodes g)
         ;; Magic number seems to be <= 2, maybe 3 with additional criteria
         ;; Also see where the heck nodes of the form "ddf.features/apps/features" are coming from
         ;; TODO The last component should be a version, not a packaging
         (filter #(= 4 (lm-gra/in-degree g %)))
         (map #(vector % (map first (lm-gra/in-edges g %))))
         (into {}))))

;; ----------------------------------------------------------------------
;; # Graphing experiments
;;
;; Define, document, and iterate on your graphs here.

(comment
  "List of DDF bundle names"
  (->> (osgi/bundles)
       (filter (b/select-bundles-built-by (user-name)))
       (map :name)))

(comment
  "List of DDF bundle names partitioned into name groups"
  (let [pars bundle-pars]
    (->> (osgi/bundles)
         (filter (b/select-bundles-built-by (user-name)))
         (b/partition-bundles-by-name pars)
         (map (fn [coll] (map #(:name %) coll))))))

(comment
  "Graph of the catalog bundles, limiting dependencies to DDF packages only."
  (let [bundles (->> (osgi/bundles)
                     (filter
                       (b/select-on-all
                         [(b/select-bundles-built-by (user-name))
                          (b/select-bundles-by-name "catalog-")])))]
    (view-after-save
      (dot/digraph
        (into
          [(dot/subgraph :edges (layer-create-edge
                                  (b/select-packages-ddf-only) bundles))]
          (layer-bulkcreate-nodes bundles)))
      {:format :svg :layout :dot})))

(comment
  (lm-io/view
    (lm-gen/gen-rand (lm-gra/digraph) 10 10))
  (let [g1 (lm-gra/digraph ["a" "b"] ["b" "c"] ["c" "e"])
        g2 (lm-gra/digraph ["g" "f"] ["f" "b"] ["b" "c"] ["c" "d"])
        g (lm-gra/digraph g1 g2)
        g-sub (lm-der/subgraph-reachable-from g "g")
        g-sur (lm-der/surroundings g #{"f" "c"})
        g-nodes (-> g (lm-alg/pre-traverse "a") (set))]
    (->> (lm-gra/nodes g)
         (filter #(= 0 (lm-gra/out-degree g %)))))
  (->> (lm-gra/nodes g)
       (filter #(= 0 (lm-gra/in-degree g %))))
  (lm-io/view g :fmt :svg)
  (lm-der/nodes-filtered-by #(contains? g-nodes %) g)
  #_(doall)
  #_(lm-gra/digraph)
  #_(lm-alg/connect)
  #_(lm-alg/bf-span "a")
  #_(lm-gra/predecessors "b"))

(comment
  "Sample graph of Maven dependencies using Loom for more fine-grained control."
  (let [edges
        (->> m/my-output-dir
             m/mvn-list-dep-files
             #_(filter #(or (.contains % "/security-") (.contains % "/platform-security-")))
             m/mvn-collect-edges
             (filter #(not= (:name %) "test"))
             m/mvn-edges-as-adjacency-list
             (filter #(let [target (get % 1)]
                        (or (.startsWith target "ddf")
                            (.startsWith target "org.codice")
                            (.startsWith target "com.connexta")))))
        g (apply lm-gra/digraph edges)
        gt (lm-gra/transpose g)
        g* (lm-der/subgraph-reachable-from gt "com.google.guava/guava/20.0")]
    (lm-io/view g* :fmt :svg)
    #_(lm-io/view g :fmt :svg)
    #_(->> (lm-gra/nodes g)
           (filter #(.contains % "guava")))
    #_(->> (lm-gra/nodes g)
           (filter #(= 0 (lm-gra/in-degree g %)))))

  "Sample graph of Maven dependencies using GraphViz APIs directly."
  (let [edges
        (->> m/my-output-dir
             m/mvn-list-dep-files
             (filter #(or (.contains % "/security-") (.contains % "/platform-security-")))
             m/mvn-collect-edges
             (filter #(not= (:name %) "test"))
             m/mvn-edges-as-adjacency-list
             (filter #(let [t (get % 1)]
                        (or (.startsWith t "ddf")
                            (.startsWith t "org.codice")
                            (.startsWith t "com.connexta")))))]
    (view-after-save
      (dot/digraph
        [(dot/subgraph
           :edges (into [{} (dot/edge-attrs {:color :black})] edges))]))))

(comment
  "Basic graphviz data sample"
  (view-after-save [
                    ; Define the nodes
                    [:a {}]
                    [:b]
                    [:c]
                    ; Define the edges
                    [:a :b]
                    [:a :c]
                    [:b :c {:arrowhead :empty}]]))

(comment
  "More advanced graphviz data sample, from http://www.graphviz.org/content/cluster"
  (view-after-save
    (dot/digraph
      [(dot/subgraph
         :cluster_0 [{:style :filled, :color :lightgrey, :label "platform"}
                     (dot/node-attrs {:style :filled, :color :white})
                     [:a0] [:a1] [:a2] [:a3]])
       (dot/subgraph
         :cluster_1 [{:color :blue, :label "catalog"}
                     (dot/node-attrs {:style :filled})
                     [:b0] [:b3]])
       (dot/subgraph
         :cluster_2 [{:color :green, :label "transformer"}
                     (dot/node-attrs {:style :filled})
                     [:b1] [:b2] [:b4]])
       (dot/subgraph
         :cluster_3 [{:color :purple, :label "feature"}
                     #_(dot/node-attrs {:style :filled})
                     [:c1] [:c2] [:c3]])
       (dot/subgraph
         :bundleDeps [
                      {}
                      (dot/edge-attrs {:color :red})
                      [:a1 :b3]
                      [:b2 :a3]
                      [:a3 :a0]
                      [:a0 :> :a1 :> :a2 :> :a3]
                      [:b0 :> :b1]
                      [:b2 :> :b3]
                      [:b1 :> :b2]
                      [:b1 :> :b4]])
       (dot/subgraph
         :otherDeps [{}
                     (dot/edge-attrs {:color :black})
                     [:b4 :b2]])])
    {:format :svg :layout :dot}))