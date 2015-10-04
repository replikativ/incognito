(ns incognito.transit
  (:require [clojure.edn :as edn]
            [cognitect.transit :as transit]
            [incognito.base :refer [incognito-reader incognito-writer]])
  (:import [com.cognitect.transit.impl WriteHandlers$MapWriteHandler]))

(defn incognito-write-handler [write-handlers]
  (proxy [WriteHandlers$MapWriteHandler] []
    (tag [o] (if (isa? (type o) clojure.lang.IRecord)
               "incognito"
               (proxy-super tag o)))
    (rep [o] (if (isa? (type o)  clojure.lang.IRecord)
               (if (isa? (type o) incognito.base.IncognitoTaggedLiteral)
                 (into {} o) ;; carry on as map
                 (incognito-writer @write-handlers o))
               (proxy-super rep o)))))

(defn incognito-read-handler [read-handlers]
  (transit/read-handler
   (partial incognito-reader @read-handlers)))


(comment
  (import '[java.io ByteArrayInputStream ByteArrayOutputStream]
          '[com.cognitect.transit.impl WriteHandlers$MapWriteHandler])

  (defrecord Foo [a b])

  (with-open [baos (ByteArrayOutputStream.)]
    (let [writer (transit/writer baos :json {:handlers {java.util.Map (incognito-write-handler (atom {incognito.transit.Foo (fn [foo] (println "foo") (assoc foo :c "banana"))}))}})]
      (transit/write writer (map->Foo {:a [1 2 3] :b {:c "Fooos"}}))
      (let [bais (ByteArrayInputStream. (.toByteArray baos))
            reader (transit/reader bais :json {:handlers {"incognito" (incognito-read-handler (atom {'incognito.transit.Foo map->Foo}))}})]
        (transit/read reader)))))
