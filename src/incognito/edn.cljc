(ns incognito.edn
  (:require #?(:clj [clojure.edn :as edn]
               :cljs [cljs.reader :refer [read-string]])
            [incognito.base :refer :all]))

(defn read-string-safe [read-handlers s]
  (when s
    #?(:clj
       (edn/read-string {:readers (assoc read-handlers
                                         'incognito.base.IncognitoTaggedLiteral
                                         (partial incognito-reader read-handlers))
                         :default (fn [tag literal]
                                    (map->IncognitoTaggedLiteral {:tag tag
                                                                  :value literal}))}
                        s)
       :cljs
       (binding [cljs.reader/*tag-table* (atom (merge read-handlers
                                                      ;; HACKY reconstruct vanilla tag-table
                                                      (select-keys @cljs.reader/*tag-table*
                                                                   #{"inst" "uuid" "queue"})))
                 cljs.reader/*default-data-reader-fn*
                 (atom (fn [tag val] (partial incognito-reader read-handlers)))]
         (read-string s)))))



(comment
  (defrecord Foo [a b])

  (let [foo (map->Foo {:a [1 2 3] :b {:c "Fooos"}})]
    (->> foo
         pr-str
         (read-string-safe {})
         pr-str
         (read-string-safe #_{} {'incognito.edn.Foo map->Foo})
         #_(= foo))))
