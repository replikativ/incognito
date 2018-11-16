(ns incognito.fressian
  (:require [incognito.base :refer [incognito-reader incognito-writer IncognitoTaggedLiteral]]
            [fress.api :refer [read-object write-tag write-object]]
            [fress.reader :as r :refer [IFressianReader readInt]]
            [fress.writer :as w :refer [IFressianWriter writeInt beginClosedList endList class-sym]]
            [fress.impl.codes :as codes]
            [cljs.reader :refer [read-string
                                 *tag-table* *default-data-reader-fn*]]
            [fress.impl.buffer :as buf]
            [fress.impl.raw-input :as rawIn]))

;;add incognito write-handlers
(defn- record-writer
  [write-handlers]
  (fn [w rec]
    (let [{:keys [tag value] :as r} (if (isa? (type rec) incognito.base.IncognitoTaggedLiteral)
                                      (into {} rec)
                                      (incognito-writer @write-handlers rec))
          tag                       (-> tag
                                        str
                                        (clojure.string/replace-first  #"/" ".")
                                        symbol)]
      (write-tag w "record" 2)
      (write-object w tag)
      (write-tag w "map" 1)
      (beginClosedList w)
      (doseq [[field value] r]
        (write-object w field true)
        (write-object w value))
      (endList w))))


;;add incognito read-handler
(defn- record-reader
  [read-handlers]
  (fn [rdr tag component-count]
    (let [tag   (read-object rdr)
          value (read-object rdr)]
      (incognito-reader @read-handlers
                        {:tag tag :value (:value value)}))))

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

(defn- write-tree-map [w m]
  (write-tag w "map" 1)
  (beginClosedList w)
  (doseq [[field value] m]
    (write-object w field true)
    (write-object w value))
  (endList w))

(defn incognito-read-handlers [read-handlers]
  {"record" (record-reader read-handlers)
   "plist"   plist-reader
   "pvec"    pvec-reader
   "set"     set-reader})

(defn incognito-write-handlers [write-handlers]
  {cljs.core/List             plist-writer
   cljs.core/EmptyList        plist-writer
   cljs.core/PersistentTreeMap write-tree-map
   "record"                   (record-writer write-handlers)
   cljs.core/LazySeq          plist-writer
   cljs.core/PersistentVector pvec-writer})

(comment

  (do
   (defrecord SomeRecord [f1 f2])
   (def rec (SomeRecord.  [1 2 2] {:c "213"}))
   (def buf (fress.api/byte-stream))
   (def writer (fress.api/create-writer buf :handlers (incognito-write-handlers (atom {'incognito.fressian.SomeRecord (fn [foo] (println "foos") (assoc foo :c "donkey"))}))))
   (fress.api/write-object writer rec)
   (fress.api/read buf :handlers (incognito-read-handlers (atom {}))))

)
