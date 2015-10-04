(ns incognito.base)

(defrecord IncognitoTaggedLiteral [tag value])

(defn incognito-reader [read-handlers m]
  (if (read-handlers (:tag m))
    ((read-handlers (:tag m)) (:value m))
    (map->IncognitoTaggedLiteral m)))

(defn incognito-writer [write-handlers r]
  (let [serial (if (write-handlers (type r))
                 ((write-handlers (type r)) r)
                 r)]
    {:tag (symbol (pr-str (type r)))
     :value (into {} serial)}))

;; denotation
;; create reader and writer routines for each format
;; pr-str + read-string-safe
;; fress/read + fress/create-writer
;; transit/reader + transit/writer

;; problem is hashing: in memory tagged literals need to yield same
;; fix in hasch

;; hash tag notation is not identical with dot and /, but is
;; distinction between JVM records and cljs records


(comment
  (incognito-writer {incognito.core.Foo (fn [r] (assoc r :c "bananas"))}
                    (map->Foo {:a [1 2 3] :b {:c "Fooos"}}))

  ({incognito.core.Foo (fn [r] (assoc r :c "bananas"))} (type (map->Foo {:a [1 2 3] :b {:c "Fooos"}}))))
