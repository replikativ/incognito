(ns incognito.base
  (:require [clojure.string :as str]))

(defrecord IncognitoTaggedLiteral [tag value])

(defn incognito-reader [read-handlers m]
    (if (read-handlers (:tag m))
           ((read-handlers (:tag m)) (:value m))
           (map->IncognitoTaggedLiteral m)))

(defn cleanup-cljs-ns [s]
  (str/replace-first s "/" "."))

(defn incognito-writer [write-handlers r]
  (let [s                   (-> r type pr-str cleanup-cljs-ns symbol)
        break-map-recursion (if (map? r) (into {} r) r)
        [tag v]             (if (write-handlers s)
                              [s ((write-handlers s) break-map-recursion)]
                              [s break-map-recursion]
                              #_(pr-str->pure-read-string r))]
    {:tag   tag
     :value v}))

