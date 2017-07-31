(ns ddf.catalog.helpers
  (:import (org.osgi.framework FrameworkUtil)
           (org.apache.shiro.util ThreadContext)
           (org.codice.ddf.security.common Security)
           (ddf.catalog.data Metacard)
           (ddf.catalog.operation.impl QueryRequestImpl QueryImpl)
           (org.codice.ddf.devtools Nrepl)))

(defn bind-admin []
  (ThreadContext/bind
    (. (Security/getInstance) (getSubject "admin" "admin"))))

(defn- bundle-cxt []
  (.. FrameworkUtil (getBundle Nrepl) getBundleContext))

(defn get-services [name]
  (let [ctx (bundle-cxt)]
    (map #(. ctx (getService %))
          (. ctx (getAllServiceReferences name nil)))))

(defn get-catalog-framework []
  (first (get-services "ddf.catalog.CatalogFramework")))

(defn get-filter-builder []
  (first (get-services "ddf.catalog.filter.FilterBuilder")))

(defn get-workspace-filter []
  (.. (get-filter-builder)
      (attribute Metacard/TAGS) is like (text "workspace")))

(defn get-source-ids []
  (.. (get-catalog-framework) (getSourceIds)))

(defn query-request [filter]
  (QueryRequestImpl. (QueryImpl. filter) false))

(defn query []
  (.. (get-catalog-framework)
      (query (query-request (get-workspace-filter)))))

