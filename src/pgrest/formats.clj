(ns pgrest.formats
  (:require
   [cheshire.core :as cc]
   [clj-time.format :as tfmt]
   [clj-time.coerce :as tc]
   [hiccup.core :as html]
   [clojure.data.csv :as csv]
   [clj-yaml.core :as yaml]
   [clojure.tools.reader.edn :as edn]
   [cheshire.generate :as cg]
   [cognitect.transit :as transit])

  (:import (java.io ByteArrayInputStream ByteArrayOutputStream StringWriter)
           org.postgresql.util.PGobject))


(def date-to-json-formatter
  (tfmt/formatters :date-time))

(cg/add-encoder
 org.joda.time.DateTime
 (fn  [d json-generator]
   (.writeString json-generator
                 (tfmt/unparse date-to-json-formatter d))))

(cg/add-encoder
 org.postgresql.util.PGobject
 (fn  [d json-generator]
   (.writeString json-generator (.toString d))))

(defn from-json  [str]
  (cc/parse-string str keyword))

(defn to-json  [clj &  [options]]
  (cc/generate-string clj options))

(defn from-edn  [str] (edn/read-string str))
(defn to-edn  [clj] (pr-str clj))


(defn layout [data]
  (html/html
   [:html
    [:head
     [:link {:href "//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" :rel "stylesheet"}]
     [:link {:href "data:image/x-icon;base64,AAABAAEAEBAQAAEABAAoAQAAFgAAACgAAAAQAAAAIAAAAAEABAAAAAAAgAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAA////AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADwPwAA4B8AAOAPAADgAwAA4BkAAOAdAADwPQAA+DkAAPh7AADwMwAA4BcAAOATAADwOwAA8D8AAPM/AAD3vwAA"
             :rel "icon"
             :type "image/x-icon"}]]
    [:body
     [:div.container-fluid data]]]))

(defn html-cell [x]
  (let [s (str x)]
    (if (> (.length s) 100)
      (let [id (gensym)]
        [:span
         [:a {:href "#" :onclick (str "document.getElementById('" id "').style.display='block'")} "expand"]
         (.substring s 0 100) "..."
         [:pre {:id id :style "display: none;"} s]])
      s)))

(defn to-html  [clj]
  (let [ks (keys (first clj))]
    (layout
     [:div
      [:h1 "pgrest"]
      [:hr]
      [:table.table.table-condensed
       [:thead (for [k ks] [:th k])]
       [:tbody
        (for [row clj]
          [:tr (for [k ks]
                 [:td (if (= :links k)
                        (for [l (:links row)] [:a {:href (str (:href l) "?_format=html")} (str (:rel l)) "&nbsp;"])
                        (html-cell (get row k)))]
                 )])]]])))

(defn from-csv  [str] (csv/read-csv str))

(defn to-csv  [clj]
  (let [keys (keys (first clj))
        clj  (mapv (fn [x] (map #(get x %) keys)) clj)
        writer (StringWriter.)]
    (csv/write-csv writer clj)
    (.toString writer)))

(to-csv [{:a 1 :c 3} {:a 2}])

(defn from-yaml  [str]
  (yaml/parse-string str keyword false))

(defn to-yaml  [clj]
  (yaml/generate-string clj))

(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> v tc/to-date .getTime))
   (fn [v] (-> v tc/to-date .getTime .toString))))

(def pg-obj-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (.toString v))
   (fn [v] (.toString v))))

(def transit-opts
  {:handlers {org.joda.time.DateTime joda-time-writer
              org.postgresql.util.PGobject pg-obj-writer}})

(defn to-transit-stream [clj & [frmt]]
  (let [out (ByteArrayOutputStream.)]
    (->  out
     (transit/writer :json transit-opts)
     (transit/write clj))
    out))

(defn to-transit [clj & [frmt]]
  (.toString (to-transit-stream clj [frmt])))

(defn from-transit [str & [frmt]]
  (-> (cond
        (string? str) (ByteArrayInputStream. (.getBytes str))
        (= (type str) ByteArrayOutputStream) (ByteArrayInputStream. (.toByteArray str))
        :else str)
      (transit/reader :json)
      (transit/read)))

(defn- transit-request? [request]
  (if-let [type (:content-type request)]
    (let [mtch (re-find #"^application/transit\+(json|msgpack)" type)]
      [(not (empty? mtch)) (keyword (second mtch))])))

(comment
  (from-transit
   (to-transit-stream {:a 1} :json))

  (from-transit
   (to-transit {:a 1} :json)))
