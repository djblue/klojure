(ns ddf.catalog.core
  (:require [osgi.core :as osgi])
  (:import [org.apache.shiro.util ThreadContext]))

(defn bind-admin []
  (ThreadContext/bind
   (. (Security/getInstance) (getSubject "admin" "admin"))))

(defn get-catalog-framework []
  (first (osgi/get-services "ddf.catalog.CatalogFramework")))

(defn get-filter-builder []
  (first (osgi/get-services "ddf.catalog.filter.FilterBuilder")))

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

(comment
  (bind-admin))
