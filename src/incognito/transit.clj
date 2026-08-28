(ns incognito.transit
  (:require [cognitect.transit :as transit]
            [incognito.base :refer [incognito-reader incognito-writer]])
  (:import [com.cognitect.transit.impl WriteHandlers$MapWriteHandler]))

(defn incognito-write-handler [write-handlers]
  ;; `proxy-super` expands to `(. this <meth> ...)` with `this` untyped, so both
  ;; super calls resolve reflectively on every non-record value written. It is
  ;; expanded here instead: `proxy-call-with-super` is what performs the
  ;; unbinding that makes the call reach the superclass rather than recurse,
  ;; and writing it out is what lets the target carry a type hint.
  (proxy [WriteHandlers$MapWriteHandler] []
    (tag [o]
      (if (isa? (type o) clojure.lang.IRecord)
        "incognito"
        (proxy-call-with-super
         #(.tag ^WriteHandlers$MapWriteHandler this o) this "tag")))
    (rep [o]
      (if (isa? (type o)  clojure.lang.IRecord)
        (if (isa? (type o) incognito.base.IncognitoTaggedLiteral)
          (into {} o) ;; carry on as map
          (incognito-writer @write-handlers o))
        (proxy-call-with-super
         #(.rep ^WriteHandlers$MapWriteHandler this o) this "rep")))))

(defn incognito-read-handler [read-handlers]
  (transit/read-handler
   (partial incognito-reader @read-handlers)))

(comment
  (import '[java.io ByteArrayInputStream ByteArrayOutputStream]
          '[com.cognitect.transit.impl WriteHandlers$MapWriteHandler])

  (defrecord Foo [a b])

  (import '[com.cognitect.transit.impl WriteHandlerMap])

  (import '[com.cognitect.transit TransitFactory])

  (get (WriteHandlerMap/defaults) java.util.Map)

  (keys (WriteHandlerMap/defaults))

  (get (TransitFactory/defaultWriteHandlers) java.util.Map)

  (map pr-str (keys (TransitFactory/defaultWriteHandlers)))

  (isa? (type (Foo. 1 2)) java.util.Map)

  (with-open [baos (ByteArrayOutputStream.)]
    (let [writer (transit/writer baos :json
                                 {:handlers {clojure.lang.PersistentArrayMap (incognito-write-handler
                                                                              (atom {'incognito.transit.Foo (fn [foo] (println "foo") (assoc foo :c "banana"))}))}})]
      (transit/write writer (map->Foo {:a [1 2 3] :b {:c "Fooos"}}))
      (let [bais (ByteArrayInputStream. (.toByteArray baos))
            reader (transit/reader bais :json {:handlers {"incognito" (incognito-read-handler (atom {'incognito.transit.Foo map->Foo}))}})]
        (transit/read reader)))))
