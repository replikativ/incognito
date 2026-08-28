(ns incognito.transit-test
  (:require [clojure.test :refer [deftest testing is]]
            [cognitect.transit :as transit]
            [incognito.transit :refer [incognito-write-handler incognito-read-handler]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [com.cognitect.transit.impl WriteHandlers$MapWriteHandler]))

(defrecord Bar [a b])

(deftest super-delegation-test
  (testing "a non-record falls through to WriteHandlers$MapWriteHandler"
    ;; The two roundtrip tests below are disabled, so this is the only live
    ;; cover for the write handler. It pins the branch that delegates to the
    ;; superclass, which is the half that silently breaks if the `tag`/`rep`
    ;; super calls stop reaching it.
    ;;
    ;; Registered on APersistentMap, NOT java.util.Map: transit resolves a
    ;; Clojure map to a more specific default handler than java.util.Map, so
    ;; registering there leaves the handler installed but never called, and the
    ;; test passes no matter what the handler does.
    (let [plain {:a 1 :b {:c "nested"} :d [1 2 3]}
          json  (with-open [baos (ByteArrayOutputStream.)]
                  (let [w (transit/writer baos :json
                                          {:handlers {clojure.lang.APersistentMap
                                                      (incognito-write-handler (atom {}))}})]
                    (transit/write w plain)
                    (String. (.toByteArray baos))))]
      (is (= "[\"^ \",\"~:a\",1,\"~:b\",[\"^ \",\"~:c\",\"nested\"],\"~:d\",[1,2,3]]" json))
      (is (= plain
             (transit/read (transit/reader (ByteArrayInputStream. (.getBytes json))
                                           :json {})))))))

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
