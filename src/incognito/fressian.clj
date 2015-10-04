(ns incognito.fressian
  (:require [clojure.edn :as edn]
            [clojure.data.fressian :as fress]
            [incognito.base :refer :all])
  (:import [org.fressian.handlers WriteHandler ReadHandler]))


(defn record-reader [read-handlers]
  (reify ReadHandler
    (read [_ reader _ component-count]
      (let [len (.readInt reader)
            tag (.readObject reader)
            val (->> (range len)
                     (map (fn [_] (.readObject reader)))
                     (map vec)
                     (into {}))]
        (incognito-reader @read-handlers
                          {:tag tag :value val})))))

(defn record-writer [write-handlers]
  (reify WriteHandler
    (write [_ writer record]
      (let [{:keys [tag value]} (if (isa? (type record) incognito.base.IncognitoTaggedLiteral)
                                  (into {} record) ;; carry on as map
                                  (incognito-writer @write-handlers record))]
        (.writeTag writer "irecord" 2)
        (.writeInt writer (count value))
        (.writeObject writer tag)
        (doseq [e value]
          (.writeObject writer e))))))

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

(defn incognito-read-handlers [read-handlers]
  {"irecord" (record-reader read-handlers)
   "plist" plist-reader
   "pvec" pvec-reader})

(defn incognito-write-handlers [write-handlers]
  {clojure.lang.PersistentList {"plist" plist-writer}
   clojure.lang.PersistentList$EmptyList {"plist" plist-writer}
   clojure.lang.IRecord {"irecord" (record-writer write-handlers)}
   clojure.lang.LazySeq {"plist" plist-writer}
   clojure.lang.PersistentVector {"pvec" pvec-writer}})


(comment

  (import '[java.io ByteArrayInputStream ByteArrayOutputStream]
          '[com.cognitect.transit.impl WriteHandlers$MapWriteHandler])

  (defrecord Foo [a b])

  (with-open [baos (ByteArrayOutputStream.)]
    (let [read-handlers (atom {'incognito.fressian.Foo map->Foo})
          write-handlers (atom {incognito.fressian.Foo (fn [foo] (println "foos") (assoc foo :c "donkey"))})
          w (fress/create-writer baos :handlers (-> (merge fress/clojure-write-handlers
                                                           (incognito-write-handlers write-handlers))
                                                    fress/associative-lookup
                                                    fress/inheritance-lookup))] ;
      (fress/write-object w (map->Foo {:a [1 2 3] :b {:c "Fooos"}}))
      (let [bais (ByteArrayInputStream. (.toByteArray baos))]
        (fress/read bais
                    :handlers (-> (merge fress/clojure-read-handlers
                                         (incognito-read-handlers (atom {}) #_read-handlers))
                                  fress/associative-lookup)))))

  )
