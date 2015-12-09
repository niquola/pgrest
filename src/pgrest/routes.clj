(ns pgrest.routes
  (:require [route-map.core :as rm]
            [pgrest.db :as db]
            [pgrest.cluster :as cluster]
            [pgrest.database :as database]
            [pgrest.app :as ctrl]))


(defn $not-found [req]
  {:status  404
   :headers {"Content-Type" "text"}
   :body {:message "Not found"
          :request (pr-str req)}})

(defn api-header [req]
  {:swagger "2.0"
   :host "postgrest.dev.health-samurai.io:3000/v1"
   :schemes ["http"]
   :info {:title "pgrest"
          :description "REST api for PostgreSQL"
          :termsOfService "http://pgrest.io/terms/"
          :contact {:name "API Support"
                    :url "http://pgrest.io/support"
                    :email "support@pgrest.io"}
          :license  {:name "MIT"
                     :url "http://www.apache.org/licenses/LICENSE-2.0.html"}}
   :paths {"/" {:get {:summary "Cluster information"}}
           "/databases" {:get {:summary "List available databases"}}
           "/databases/{db}" {:get {:summary    "test database"
                                    :parameters [{:name "db"
                                                  :in "path"
                                                  :description "db name"
                                                  :required true
                                                  :type "string"}]}}
           "/databases/{db}/schemes" {:get {:summary    "test database"
                                            :parameters [{:name "db"
                                                          :in "path"
                                                          :description "db name"
                                                          :required true
                                                          :type "string"}]}}
           "/databases/{db}/schemes/{ns}" {:get {:summary    "test database"}}
           "/databases/{db}/schemes/{ns}/tables" {:get {:summary    "test database"}}
           "/databases/{db}/schemes/{ns}/views" {:get {:summary    "test database"}}
           "/databases/{db}/schemes/{ns}/functions" {:get {:summary    "test database"}}}})

(defn wrap-db [h]
  (fn [{{db-name :db-name} :params :as req}]
    (h
     (if-let [db (and db-name (db/get-datasource db-name))]
       (assoc req :db db)
       req))))

(defn $v1 [req]
  {:body (api-header req)})

(def v1
  {"swagger"   {:GET #'$v1}
   :interceptors [#'wrap-db]
   :GET        #'cluster/$index
   [:db-name]  #'database/routes})

(def routes
  {:GET $not-found
   "v1" v1})

(defn build-stack [h mws]
  ((apply comp mws) h))

(defn dispatch [{meth :request-method uri :uri :as req}]
  (println "\n\nHTTP:  " meth " "  uri " " (pr-str (:params req)))
  (if-let [rt (rm/match [meth uri] routes)]
    (let [mws           (mapcat :interceptors (:parents rt))
          params        (merge (:params req) (:params rt))
          req           (merge req {:match rt :params params})
          handle-with   (first (filter identity (map :handle-with (reverse (:parents rt)))))
          handler       (or handle-with (:match rt))]
      ((build-stack handler mws) req))
    ($not-found req)))

