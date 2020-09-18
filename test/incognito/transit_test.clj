(ns incognito.transit-test
  (:require [clojure.test :refer [deftest testing is]]
            [cognitect.transit :as transit]
            [incognito.transit :refer [incognito-write-handler incognito-read-handler]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [com.cognitect.transit.impl WriteHandlers$MapWriteHandler]))

(defrecord Bar [a b])




#_(deftest incognito-roundtrip-test
  (testing "Test incognito transport."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
      (is (= #incognito.base.IncognitoTaggedLiteral{:tag incognito.transit_test.Bar,
                                                    :value {:a [1 2 3],
                                                            :b {:c "Fooos"}
                                                            :c "banana"}}
             (with-open [baos (ByteArrayOutputStream.)]
               (let [writer (transit/writer baos :json
                                            #_{:default-handler (transit/write-handler "" (fn [& args] (println args)))}
                                            {:handlers {clojure.lang.IRecord
                                                        (incognito-write-handler
                                                         (atom {'incognito.transit_test.Bar
                                                                (fn [foo] (assoc foo :c "banana"))}))}}
                                            #_{:handlers {java.util.Map
                                                        (incognito-write-handler
                                                         (atom {'incognito.transit_test.Bar
                                                                (fn [foo] (assoc foo :c "banana"))}))}})]
                 (transit/write writer bar)
                 (let [bais (ByteArrayInputStream. (.toByteArray baos))
                       reader (transit/reader bais :json
                                              {}

                                              #_{:handlers {"incognito"
                                                          (incognito-read-handler (atom {}))}
                                               :default-handler (transit/default- (fn [& args]
                                                                                        (println "def.handler:" args)))})]
                   (transit/read reader)))))))))

#_(deftest double-roundtrip-test
  (testing "Test two roundtrips, one incognito and deserialize at end."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
      (is (= bar
             (with-open [baos (ByteArrayOutputStream.)]
               (let [writer (transit/writer baos :json
                                            {:handlers {java.util.Map
                                                        (incognito-write-handler
                                                         (atom {}))}})]
                 (transit/write writer bar)
                 (let [bais (ByteArrayInputStream. (.toByteArray baos))
                       reader (transit/reader bais :json
                                              {:handlers {"incognito"
                                                          (incognito-read-handler (atom {'incognito.transit_test.Bar map->Bar}))}})]
                   (with-open [baos (ByteArrayOutputStream.)]
                     (let [writer (transit/writer baos :json
                                                  {:handlers {java.util.Map
                                                              (incognito-write-handler
                                                               (atom {}))}})]
                       (transit/write writer (transit/read reader))
                       (let [bais (ByteArrayInputStream. (.toByteArray baos))
                             reader (transit/reader bais :json
                                                    {:handlers {"incognito"
                                                                (incognito-read-handler (atom {'incognito.transit_test.Bar map->Bar}))}})]
                         (transit/read reader))))))))))))
