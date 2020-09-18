(ns incognito.base
  (:require [clojure.string :as str]))

(defrecord IncognitoTaggedLiteral [tag value])

(defn incognito-reader [read-handlers m]
    (if (read-handlers (:tag m))
           ((read-handlers (:tag m)) (:value m))
           (map->IncognitoTaggedLiteral m)))

(defn normalize-ns [s]
  (-> s
     (str/replace-first "/" ".")
     (str/replace "-" "_")))

(defn incognito-writer [write-handlers r]
  (let [s                   (-> r type pr-str normalize-ns symbol)
        break-map-recursion (if (map? r) (into {} r) r)
        [tag v]             (if (write-handlers s)
                              [s ((write-handlers s) break-map-recursion)]
                              [s break-map-recursion]
                              #_(pr-str->pure-read-string r))]
    {:tag   tag
     :value v}))

