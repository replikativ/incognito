(ns incognito.fressian
  (:require [incognito.base :refer [incognito-reader incognito-writer]]
            [fress.api :refer [read-object write-tag write-object]]
            [fress.reader :refer [IFressianReader readInt]]
            [fress.writer :refer [IFressianWriter writeInt]]))

(defn- record-reader [read-handlers reader]
  (let [len (readInt reader)
        tag (read-object reader)
        val (->> (range len)
                 (map (fn [_] (read-object reader)))
                 (map vec)
                 (into {}))]
    (incognito-reader @read-handlers
                      {:tag tag :value val})))

(defn- record-writer [write-handlers writer record]
  (let [{:keys [tag value]} (if (isa? (type record) incognito.base.IncognitoTaggedLiteral)
                              (into {} record) ;; carry on as map
                              (incognito-writer @write-handlers record))]
    (write-tag writer "irecord" 2)
    (writeInt writer (count value))
    (write-object writer tag)
    (doseq [e value]
      (write-object writer e))))

(defn- plist-reader [reader _ _]
  (let [len (readInt reader)]
    (->> (range len)
         (map (fn [_] (read-object reader)))
         reverse
         (into '()))))

(defn- plist-writer [writer plist]
  (write-tag writer "plist" 2)
  (writeInt writer (count plist))
  (doseq [e plist]
    (write-object writer e)))

(defn- pvec-reader [reader _ _]
  (let [len (readInt reader)]
    (->> (range len)
         (map (fn [_] (read-object reader)))
         (into []))))

(defn- pvec-writer [writer pvec]
  (write-tag writer "pvec" 2)
  (writeInt writer (count pvec))
  (doseq [e pvec]
    (write-object writer e)))

(defn- set-reader [reader _ _]
  (let [^List l (read-object reader)]
        (into #{} l)))

(defn incognito-read-handlers [read-handlers]
  {"irecord" (partial record-reader read-handlers)
   "plist"   plist-reader
   "pvec"    pvec-reader
   "set"     set-reader})

(defn incognito-write-handlers [write-handlers]
  {cljs.core/List             plist-writer
   cljs.core/EmptyList        plist-writer
   cljs.core/IRecord          (partial record-writer write-handlers)
   cljs.core/LazySeq          plist-writer
   cljs.core/PersistentVector pvec-writer})

(comment

  (defrecord Foo [a b])

  (let [buffer         (fress.api/byte-stream)
        read-handlers  (atom {'incognito.fressian.Foo map->Foo})
        write-handlers (atom {'incognito.fressian.Foo (fn [foo] (println "foos") (assoc foo :c "donkey"))})
        w              (fress.api/create-writer buffer :handlers (incognito-write-handlers write-handlers))]
    (fress.api/write-object w #{4 2 3})
    (fress.api/read buffer
                          :handlers (incognito-read-handlers read-handlers)))

  )
