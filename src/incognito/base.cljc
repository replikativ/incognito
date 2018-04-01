(ns incognito.base
  #?(:clj (:refer-clojure :exclude [read-string]))
  (:require #?(:clj [clojure.edn :refer [read-string]]
               :cljs [cljs.reader :refer [read-string
                                          *tag-table* *default-data-reader-fn*]])))

(defrecord IncognitoTaggedLiteral [tag value])

#_(defn pr-str->pure-read-string [r]
  (let [r-str (pr-str r)]
    #?(:cljs (binding [*tag-table* (atom (select-keys @cljs.reader/*tag-table*
                                                      ["inst" "uuid" "queue" "js"]))
                       *default-data-reader-fn* (atom (fn [t v] [t v]))]
               (read-string r-str))
       :clj (read-string {:readers {} :default (fn [t v] [t v])} r-str))))

(defn incognito-reader [read-handlers m]
  (if (read-handlers (:tag m))
    ((read-handlers (:tag m)) (:value m))
    (map->IncognitoTaggedLiteral m)))

(defn incognito-writer [write-handlers r]
  (let [s (-> r type pr-str symbol)
        break-map-recursion (if (map? r) (into {} r) r)
        [tag v] (if (write-handlers s)
                  [s ((write-handlers s) break-map-recursion)]
                  [s break-map-recursion]
                  #_(pr-str->pure-read-string r))]
    {:tag tag
     :value v}))



(comment
  (require '[clj-time.core :as t])
  (require '[clj-time.format :as tf])

  (incognito-writer {'org.joda.time.DateTime
                     (fn [r] (str r))}
                    (t/now))

  (incognito-reader {'org.joda.time.DateTime
                     (fn [r] (t/date-time r))}
                    {:tag 'org.joda.time.DateTime, :value "2017-04-17T13:11:29.977Z"})





  (type (t/now))


  (t/now)

  (defrecord Foos [a])

  (into {} (map->Foos {:a 4}))

  (cljs-type (map->Foos {:a 4}))



  (incognito-writer {'incognito.base.Foos (fn [r] (assoc r :c "bananas"))}
                    (map->Foos {:a [1 2 3] :b {:c "Fooos"}}))

  (incognito-writer {} (map->Foos {:a [1 2 3] :b {:c "Fooos"}}))

  )
