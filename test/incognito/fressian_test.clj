(ns incognito.fressian-test
  (:require [clojure.test :refer :all]
            [clojure.data.fressian :as fress]
            [incognito.fressian :refer :all])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [org.fressian.handlers WriteHandler ReadHandler]))

(defrecord Bar [a b])

(deftest incognito-roundtrip-test
  (testing "Test incognito transport."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
      (is (= #incognito.base.IncognitoTaggedLiteral{:tag incognito.fressian-test/Bar,
                                                    :value {:a [1 2 3],
                                                            :b {:c "Fooos"}
                                                            :c "donkey"}}
             (with-open [baos (ByteArrayOutputStream.)]
               (let [write-handlers (atom {'incognito.fressian-test/Bar
                                           (fn [bar] (assoc bar :c "donkey"))})
                     w (fress/create-writer baos
                                            :handlers
                                            (-> (merge fress/clojure-write-handlers
                                                       (incognito-write-handlers write-handlers))
                                                fress/associative-lookup
                                                fress/inheritance-lookup))]
                 (fress/write-object w bar)
                 (let [bais (ByteArrayInputStream. (.toByteArray baos))]
                   (fress/read bais
                               :handlers
                               (-> (merge fress/clojure-read-handlers
                                          (incognito-read-handlers (atom {})))
                                   fress/associative-lookup))))))))))

(deftest double-roundtrip-test
  (testing "Test two roundtrips, one incognito and deserialize at end."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
      (is (= bar
             (with-open [baos (ByteArrayOutputStream.)]
               (let [read-handlers (atom {'incognito.fressian-test/Bar map->Bar})
                     w (fress/create-writer baos
                                            :handlers
                                            (-> (merge fress/clojure-write-handlers
                                                       (incognito-write-handlers (atom {})))
                                                fress/associative-lookup
                                                fress/inheritance-lookup))]
                 (fress/write-object w bar)
                 (let [bais (ByteArrayInputStream. (.toByteArray baos))]
                   (with-open [baos (ByteArrayOutputStream.)]
                     (let [w (fress/create-writer baos
                                                  :handlers
                                                  (-> (merge fress/clojure-write-handlers
                                                             (incognito-write-handlers (atom {})))
                                                      fress/associative-lookup
                                                      fress/inheritance-lookup))]
                       (fress/write-object w (fress/read bais
                                                         :handlers
                                                         (-> (merge fress/clojure-read-handlers
                                                                    (incognito-read-handlers (atom {})))
                                                             fress/associative-lookup)))
                       (let [bais (ByteArrayInputStream. (.toByteArray baos))]
                         (fress/read bais
                                     :handlers
                                     (-> (merge fress/clojure-read-handlers
                                                (incognito-read-handlers read-handlers))
                                         fress/associative-lookup)))))))))))))
