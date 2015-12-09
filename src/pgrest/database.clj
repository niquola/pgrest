(ns pgrest.database
  (:require [clj-pg.honey :as hsql]
            [clojure.string :as str]
            [honeysql.core :as hsqlc]
            [honeysql.helpers :as hsqlh]
            [clojure.walk :as walk]
            [pgrest.db :as db]
            [pgrest.formats :as f]
            [pgrest.schemes :as sch]
            [pgrest.simpleq :as sq]
            [pgrest.utils :as u]))

(defn param-key [x]
  (keyword (str/replace (name x) #"^(keyword)?->" "")))

(defn is-param? [x]
  (and (keyword? x) (.startsWith (name x) "->")))

(defn is-key-param? [x]
  (and (keyword? x) (.startsWith (name x) "keyword->")))

(defn replace-params [params query]
  (walk/postwalk (fn [x]
                   (cond
                     (is-key-param? x)
                     (if-let [v (get params (param-key x))]
                       (keyword v) x)
                     (is-param? x)
                     (if-let [v (get params (param-key x))] v x)
                     :else x))
                 query))

(defn decorate [path links item]
  (assoc item :links
         (mapv (fn [[k v]]
                 {:rel (name k)
                  :href (apply u/url path (replace-params item v))}) links)))

(defn simple-query [q expr]
  (if-let [+exp (and q (sq/to-hsql q))]
    (assoc expr :where (:where +exp))
    expr))

(defn generic-handler [{pth :path params :params db :db match :match :as req}]
  (let [match (replace-params params match)]
    (->>
     (get-in match [:match :query])
     (apply hsqlc/build)
     (simple-query (:q params))
     (hsql/query db)
     (mapv #(decorate pth (get-in match [:match :links]) %))
     (assoc {} :body))))

(def routes
  {:handle-with #'generic-handler
   :GET      {:table :pg_namespace
              :query [:select :* :from [:pg_namespace]]
              :links {:database  [:..]
                      :tables    [:. :->nspname]
                      :functions [:. :->nspname "views"]
                      :types     [:. :->nspname "types"]}}

   [:schema] {:GET {:table :pg_tables
                    :query [:select :* :from :pg_tables :where [:= :schemaname :->schema]]
                    :links {:schema [:..] 
                            :data   [:. :->tablename]}}

              [:table] {:GET  {:table ::->table
                               :query [:select :* :from :keyword->table :limit 200]
                               :links {:tables [:..]}}}
              }})

(replace-params {:table "test"} (get-in routes [[:schema] [:table] :GET]))

(name :keyword->key)
