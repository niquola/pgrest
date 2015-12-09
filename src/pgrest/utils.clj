(ns pgrest.utils
  (:require [clojure.string :as str]
             [ring.util.codec :as codec]))

(defn butlastv [xs]
  (into [] (butlast xs)))

(defn url [pth & xs]
  (if (map? (last xs))
    (str (apply url pth (butlastv xs)) "?" (codec/form-encode (last xs)))
    (->>
     xs
     (reduce (fn [cpth x]
               (if (empty? cpth)
                 (cond
                   (= x :.)   pth
                   (= x :..)  (butlastv pth)
                   :else      [x])
                 (cond
                   (= x :.)   cpth
                   (= x :..)  (butlastv cpth)
                   :else      (conj cpth x))))
             [])
     (interpose "/")
     (apply str "/"))))

(defn uri-to-path [uri]
  (->
   uri
   (str/replace #"^/" "")
   (str/split #"/")))

(uri-to-path "/v1/databases")

(codec/form-encode {:a 1 :b 2})

(defn wrap-path [h]
  (println "WRAP PATH")
  (fn [{uri :uri :as req}]
    (println "PATH:" (uri-to-path uri))
    (h (assoc req :path (uri-to-path uri)))))

(comment
  (url ["a" "b"] "one" "two")
  (url ["a" "b"] :. "one" "two")
  (url ["a" "b"] :.. "one" "two" {:x 1})
  )

