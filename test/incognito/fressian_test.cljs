(ns incognito.fressian-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests async use-fixtures]]
            [incognito.fressian :refer [incognito-read-handlers incognito-write-handlers]]))

(enable-console-print!)

(defrecord Bar [a b])

(deftest incognito-roundtrip-test
  (testing "Test incognito transport."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})
          r   (let [buffer (fress.api/byte-stream)

                    read-handlers  (atom {'incognito.fressian-test.Bar map->Bar})
                    write-handlers (atom {'incognito.fressian-test.Bar
                                          (fn [bar] (assoc bar :c "donkey"))})
                    w              (fress.api/create-writer buffer :handlers (incognito-write-handlers write-handlers))]
                (fress.api/write-object w bar)
                (fress.api/read buffer :handlers (incognito-read-handlers (atom {}))))]
      (is (= {:c "donkey", :b {:c "Fooos"}, :a [1 2 3]} (:value r)))
      (is (= 'incognito.fressian-test.Bar (:tag r)))
      (is (= incognito.base/IncognitoTaggedLiteral (type r))))))

(deftest double-roundtrip-test
  (testing "Test two roundtrips, one incognito and deserialize at end."
    (let [bar            (map->Bar {:a [1 2 3] :b {:c "Fooos"}})
          write-handlers (atom {})
          read-handlers  (atom {'incognito.fressian-test.Bar map->Bar})]
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
