(ns pgrest.simpleq
  (:require [clojure.string :as str]))


(defn to-op [col ops]
  (reduce (fn [acc [k v]]
            (cond
              (= k :like) (conj acc [:like col (str "%" v "%")])
              (= k :ilike) (conj acc [:ilike col (str "%" v "%")])
              (= k :lt) (conj acc [:< col v])
              (= k :gt) (conj acc [:> col v])
              :else acc))
          [:and] ops))

(to-op :a {:like "b"})
  

(defn to-hsql [params]
  (->> {:select (when-let [sel (:_select params)]
                  (mapv (comp keyword #(.trim %)) sel))
        :where (reduce-kv (fn [acc k v]
                            (if-not (.startsWith (name k) "_")
                              (cond
                                (map? v) (conj acc (to-op k v))
                                :else    (conj acc [:= k v]))
                              acc))
                          [:and ] params)}
       ((fn [x] (if (:select x) x (dissoc x :select))))
       ((fn [x] (if (= [:and] (:where x)) (dissoc x :where) x)))))

(to-hsql {:a 1 :_select ["a"]})

(to-hsql {:a {:lt "b"}})
