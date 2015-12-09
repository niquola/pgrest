(ns pgrest.app
  (:require
   [pgrest.db :as db]
   [pgrest.formats :as f]
   [pgrest.simpleq :as sq]
   [pgrest.schemes :as sch]
   [clj-pg.honey :as hsql]))

(defn to-url [& xs]
  (apply str "/" (interpose "/" xs)))

(defn decorate-schema [db sch]
  (merge sch {:links [{:rel "root" :href "/"}
                      {:rel "tables" :href (to-url db (:nspname sch))}
                      {:rel "views" :href (to-url db (:nspname sch) "views")}
                      {:rel "functions" :href (to-url db (:nspname sch) "functions")}
                      {:rel "types" :href (to-url db (:nspname sch) "types")}]}))

(defn $schemes [{db :db :as req}]
  (->>
   (hsql/query db :select :* :from :pg_namespace)
   (map #(decorate-schema db %))
   (assoc {} :body)))



(defn decorate-table [db sch t]
  (merge t {:links [{:rel "data" :href (to-url db sch (:tablename t))}
                    {:rel "schema" :href (to-url db sch (:tablename t) "schema")}]}))

(defn $schema-index [{db :db {sch :schema} :params :as req}]
  {:body  (->> (hsql/query db :select :* :from :pg_tables :where [:= :schemaname sch])
                 (map (fn [x] (decorate-table db sch x))))})

(defn $table-schema [{db :db {sch :schema tbl :table} :params :as req}]
   {:body  (->> (sch/schema-for-table db sch tbl) 
                 (map (fn [x] (decorate-table db sch x))))})

(defn decorate-view [db sch t]
  (merge t {:links [{:rel "data" :href (to-url db sch (:viewname t))}]}))

(defn $views [{db :db {sch :schema} :params :as req}]
   {:body  (->> (hsql/query db :select :* :from :pg_views :where [:= :schemaname sch])
                 (map (fn [x] (decorate-view db sch x))))})

(defn decorate-function [db x] x)

(defn $functions [{db :db {sch :schema :as params} :params :as req}]
  (->>
   (hsql/query db :select [:pr.*]
               :from [[:pg_proc :pr]]
               :join [[:pg_catalog.pg_namespace :n]
                      [:= :n.oid :pr.pronamespace]]
               :where [:= :n.nspname sch])
   (map #(decorate-function db %))
   (assoc {} :body)))


(defn $table-index [{db :db {sch :schema tbl :table :as params} :params :as req}]
  (let [q (dissoc params :db-name :schema :table)
        hq {:select [:*] :from [(keyword (str sch "." tbl))]}
        hq (if (empty? q) hq (merge hq (sq/to-hsql q)))]
    {:body (->> (hsql/query db hq))}))

(def routes
  {:GET      #'$schemes
   [:schema] {:GET        #'$schema-index
              "functions" {:GET #'$functions}
              "views" {:GET #'$views}
              [:table] {:GET #'$table-index}}})
