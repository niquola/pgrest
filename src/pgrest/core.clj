(ns pgrest.core
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.resource :as rmr]
            [pgrest.routes :refer [dispatch]]
            [pgrest.db :as db]
            [pgrest.utils :as u]
            [pgrest.formats :as fmt]
            [org.httpkit.server :as ohs])
  (:gen-class))

(defn env [k])
(def defaults (merge site-defaults {:security {:anti-forgery false}}))

(def formats {"json"     [#'fmt/to-json "application/json"]
              "yaml"     [#'fmt/to-yaml "text/plain"]
              "html"     [#'fmt/to-html "text/html"]
              "edn"      [#'fmt/to-edn "text/plain"]
              "transit"  [#'fmt/to-transit "application/transit+json"]
              "csv"      [#'fmt/to-csv "text/plain"]})

(defn wrap-format [h]
  (fn [{{fmt :_format} :params :as req}]
    (let [{headers :headers body :body :as res} (h (update-in req [:params] dissoc :_format))
          [encoder content-type] (get formats (or fmt "json"))]
      (if (or (string? body) (nil? body))
        res
        (merge res {:body (encoder body)
                    :headers (merge (or headers {}) {"Content-Type" content-type})})))))


(def app (-> #'dispatch
             (wrap-format)
             (u/wrap-path)
             (wrap-defaults defaults)
             (rmr/wrap-resource "public")))

(defn start []
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (ohs/run-server #'app {:port port})))

(defn -main [] (start))

(comment
  (stop)
  (def stop (start)))
