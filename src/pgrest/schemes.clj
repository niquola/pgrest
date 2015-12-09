(ns pgrest.schemes
  (:require [pgrest.db :as db]
            [clj-pg.honey :as hsql]))


(defn schema-for-table [db-name sch tbl]
  (let [db (db/get-datasource db-name)]
    (hsql/query db
                :select :*
                :from :information_schema.columns
                :where [:and
                        [:= :table_schema (name sch)]
                        [:= :table_name (name tbl)]])))


(keys (first (schema-for-table :test :public :users)))

(first
 (hsql/query (db/get-datasource :test)
             :select :*
             :from :pg_class))

