(ns incognito.transit
  (:require [cognitect.transit :as transit]
            [incognito.base :refer [incognito-reader incognito-writer]])
  (:import [com.cognitect.transit.impl WriteHandlers$MapWriteHandler]
           [com.cognitect.transit WriteHandler]))

(defrecord WriteRecords [tag value])

(defn incognito-write-record []
  {WriteRecords
   (proxy [WriteHandlers$MapWriteHandler] []
     (tag [_] "incognito")
     (rep [o] (into {} o)))})

(defn incognito-read-handler [read-handlers]
  (transit/read-handler
   (partial incognito-reader @read-handlers)))

(defn incognito-write-handler
  "For :transform. Will write any metadata present on the value."
  [write-handlers o]
  (if (record? o)
    (if (isa? (type o) incognito.base.IncognitoTaggedLiteral)
      (into {} o)
      (let [{:keys [tag value]} (incognito-writer @write-handlers o)]
        (WriteRecords. tag value)))
    o))

