(ns incognito.base
  #?(:clj (:refer-clojure :exclude [read-string]))
  (:require #?(:clj [clojure.edn :refer [read-string]]
               :cljs [cljs.reader :refer [read-string
                                          *tag-table* *default-data-reader-fn*]])))

(defrecord IncognitoTaggedLiteral [tag value])

(defn incognito-reader [read-handlers m]
  (if (read-handlers (:tag m))
    ((read-handlers (:tag m)) (:value m))
    (map->IncognitoTaggedLiteral m)))

(defn incognito-writer [write-handlers r]
  #?(:cljs (let [wh                  (into {} (map (fn [[k v]]
                                                     [(-> k
                                                          name
                                                          (clojure.string/replace-first  #"(?s)(.*)(\.)" "$1/")
                                                          symbol)
                                                      v])
                                                   write-handlers))
                 s                   (-> r type pr-str symbol)
                 break-map-recursion (if (map? r) (into {} r) r)
                 [tag v]             (if (wh s)
                                       [s ((wh s) break-map-recursion)]
                                       [s break-map-recursion])]
             {:tag   tag
              :value v})
     :clj (let [s                   (-> r type pr-str symbol)
                break-map-recursion (if (map? r) (into {} r) r)
                [tag v]             (if (write-handlers s)
                                      [s ((write-handlers s) break-map-recursion)]
                                      [s break-map-recursion]
                                      #_(pr-str->pure-read-string r))]
            {:tag   tag
             :value v})))





