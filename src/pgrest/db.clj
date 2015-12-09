(ns pgrest.db
  (:require
   [clojure.string :as cs]
   [clj-pg.pool :as poole]
   [clj-pg.honey :as hsql]
   [clj-pg.coerce :as coerce]
   [clj-time.coerce :as tc])
  (:import org.postgresql.util.PGobject))

(defonce datasources (atom {}))

(defn build-connection-string [db-name]
  (cs/replace "jdbc:postgresql://localhost:5432/DB_NAME?user=root&password=root&stringtype=unspecified" #"DB_NAME" (name db-name)))

(defn datasource-options [db-name]
  {:idle-timeout       1000
   :minimum-idle       0
   :maximum-pool-size  2
   :connection-init-sql "SET plv8.start_proc = 'plv8_init'"
   :data-source.url   (build-connection-string db-name)  })

(defn shutdown-connections []
  (doseq [[nm {conn :datasource}] @datasources]
    (poole/close-pool conn))
  (reset! datasources {}))

(defn shutdown-connections-for-db [db-name]
  (when-let [{conn :datasource} (get @datasources db-name)]
    (poole/close-pool conn)
    (swap! datasources dissoc db-name)))

(defn get-datasource [db-name]
  (if (get @datasources db-name)
    (get @datasources db-name)
    (let [pool {:datasource (poole/create-pool db-name (datasource-options db-name))}]
      (swap! datasources assoc db-name pool)
      pool)))

(defn to-pg [type value]
  (doto (PGobject.)
    (.setType (name type))
    (.setValue (str value))))

(comment
  (let [db (get-datasource :test)]
    (hsql/query db
     :select :datname
     :from :pg_database)))

