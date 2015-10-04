(ns incognito.base)

(defrecord IncognitoTaggedLiteral [tag value])

(defn cljs-type [r]
  (-> (type r)
      pr-str
      (.replace "_" "-")
      symbol))

(defn incognito-reader [read-handlers m]
  (if (read-handlers (:tag m))
    ((read-handlers (:tag m)) (:value m))
    (map->IncognitoTaggedLiteral m)))

(defn incognito-writer [write-handlers r]
  (let [t (cljs-type r)
        serial (if (write-handlers t)
                 ((write-handlers t) r)
                 r)]
    {:tag t
     :value (into {} serial)}))

(comment
  (defrecord Foos [a])

  (cljs-type (map->Foos {:a 4}))

  (incognito-writer {'incognito.base.Foos (fn [r] (assoc r :c "bananas"))}
                    (map->Foos {:a [1 2 3] :b {:c "Fooos"}}))

  ({'incognito.base/Foos (fn [r] (assoc r :c "bananas"))} (cljs-type (map->Foos {:a [1 2 3] :b {:c "Fooos"}}))))
