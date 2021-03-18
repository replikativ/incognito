(ns incognito.fressian-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests async use-fixtures]]
            [incognito.fressian :refer [incognito-read-handlers incognito-write-handlers]]))

(enable-console-print!)

(defrecord Bar [a b])

(deftest incognito-roundtrip-test
  (testing "Test incognito transport."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})
          write-handlers (atom {'incognito.fressian_test.Bar
                                (fn [bar] (assoc bar :c "donkey"))})

          unknown (let [buffer (fress.api/byte-stream)
                        w      (fress.api/create-writer buffer :handlers (incognito-write-handlers write-handlers))]
                    (fress.api/write-object w bar)
                    (fress.api/read buffer :handlers (incognito-read-handlers (atom {}))))]
      (is (= {:c "donkey", :b {:c "Fooos"}, :a [1 2 3]} (:value unknown)))
      (is (= 'incognito.fressian_test.Bar (:tag unknown)))
      (is (= incognito.base/IncognitoTaggedLiteral (type unknown))))))

(deftest double-roundtrip-test
  (testing "Test two roundtrips, one incognito and deserialize at end."
    (let [bar            (map->Bar {:a [1 2 3] :b {:c "Fooos"}})
          write-handlers (atom {})
          read-handlers  (atom {'incognito.fressian_test.Bar map->Bar})]
      (is (= bar
             (let [buffer (fress.api/byte-stream)
                   w      (fress.api/create-writer buffer
                                                   :handlers
                                                   (incognito-write-handlers write-handlers))]
               (fress.api/write-object w bar)
               (fress.api/write-object w (fress.api/read buffer
                                                         :handlers
                                                         (incognito-read-handlers (atom {}))))
               (fress.api/read buffer
                               :handlers (incognito-read-handlers read-handlers))))))))

(run-tests)
