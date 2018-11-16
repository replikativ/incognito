(ns incognito.fressian
  (:require [clojure.edn :as edn]
            [clojure.data.fressian :as fress]
            [incognito.base :refer [incognito-reader incognito-writer]])
  (:import [org.fressian StreamingWriter]
           [org.fressian.handlers WriteHandler ReadHandler]))

(defn record-reader [read-handlers]
  (reify ReadHandler
    (read [_ reader tag component-count]
      (let [tag (.readObject reader)
            val (.readObject reader)]
        (incognito-reader @read-handlers
                          {:tag tag :value (:value val)})))))

(defn record-writer [write-handlers]
  (reify WriteHandler
    (write [_ w rec]
      (let [{:keys [tag] :as r} (if (isa? (type rec) incognito.base.IncognitoTaggedLiteral)
                                  (into {} rec) ;; carry on as map
                                  (incognito-writer @write-handlers rec))]
        (.writeTag w "record" 2)
        (.writeObject w tag)
        (.writeTag w "map" 1)
        (.beginClosedList ^StreamingWriter w)
        (doseq [[field value] r]
          (.writeObject w field true)
          (.writeObject w value))
        (.endList ^StreamingWriter w)))))

(def plist-reader
  (reify ReadHandler
    (read [_ reader tag component-count]
      (let [len (.readInt reader)]
        (->> (range len)
             (map (fn [_] (.readObject reader)))
             reverse
             (into '()))))))

(def plist-writer
  (reify WriteHandler
    (write [_ writer plist]
      (.writeTag writer "plist" 2)
      (.writeInt writer (count plist))
      (doseq [e plist]
        (.writeObject writer e)))))

(def pvec-reader
  (reify ReadHandler
    (read [_ reader tag component-count]
      (let [len (.readInt reader)]
        (->> (range len)
             (map (fn [_] (.readObject reader)))
             (into []))))))

(def pvec-writer
  (reify WriteHandler
    (write [_ writer pvec]
      (.writeTag writer "pvec" 2)
      (.writeInt writer (count pvec))
      (doseq [e pvec]
        (.writeObject writer e)))))

(def set-reader
  (reify ReadHandler
    (read [_ reader tag component-count]
      (let [^List l (.readObject reader)]
        (into #{} l)))))

(defn incognito-read-handlers [read-handlers]
  {"record" (record-reader read-handlers)
   "plist"  plist-reader
   "pvec"   pvec-reader
   "set"    set-reader})

(defn incognito-write-handlers [write-handlers]
  {clojure.lang.PersistentList           {"plist" plist-writer}
   clojure.lang.PersistentList$EmptyList {"plist" plist-writer}
   clojure.lang.IRecord                  {"record" (record-writer write-handlers)}
   clojure.lang.LazySeq                  {"plist" plist-writer}
   clojure.lang.PersistentVector         {"pvec" pvec-writer}})


(comment

  (import '[java.io ByteArrayInputStream ByteArrayOutputStream]
          '[com.cognitect.transit.impl WriteHandlers$MapWriteHandler])

  (defrecord Foo [a b])

  (with-open [baos (ByteArrayOutputStream.)]
    (let [read-handlers (atom {'incognito.fressian.Foo map->Foo})
          write-handlers (atom {'incognito.fressian.Foo (fn [foo]
                                                          (println "foos")
                                                          (assoc foo :c "donkey"))})
          w (fress/create-writer baos :handlers (-> (merge fress/clojure-write-handlers
                                                           (incognito-write-handlers write-handlers))
                                                    fress/associative-lookup
                                                    fress/inheritance-lookup))] ;
      (fress/write-object w (map->Foo {:a [1 2 3] :b {:c "Fooos"}}))
      (let [bais (ByteArrayInputStream. (.toByteArray baos))]
        (prn (fress/read bais
                          :handlers (-> (merge fress/clojure-read-handlers
                                               (incognito-read-handlers #_(atom {}) read-handlers))
                                        fress/associative-lookup))))))











  )
