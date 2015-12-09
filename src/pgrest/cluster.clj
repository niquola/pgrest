(ns pgrest.cluster
  (:require
   [pgrest.simpleq :as sq]
   [pgrest.db :as db]
   [pgrest.utils :as u]
   [clj-pg.honey :as hsql]))

(defn decorate-database [pth x]
  (println "PATH:" pth)
  (merge x {:links [{:rel "database"
                     :href (u/url pth :. (:datname x))}]}))

(u/url ["v1"] :. "ups" "dups" {:a 1})

(defn $index [{pth :path}]
  (-> :postgres
      (db/get-datasource)
      (hsql/query :select :* :from :pg_database :where [:= :datistemplate false])
      (->> (map #(decorate-database pth %))
           (assoc {} :body))))

(let [users {:users [{:id  1} {:id 2}]}]
  (update users :users
             (fn [us] (map #(assoc %1 :prop %2) us [7 8]))))



