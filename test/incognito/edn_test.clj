(ns incognito.edn-test
  (:require [clojure.test :refer :all]
            [incognito.edn :refer :all]))

(defrecord Bar [a b])

(deftest incognito-roundtrip-test
  (testing "Test incognito transport."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
      (is (= #incognito.base.IncognitoTaggedLiteral{:tag incognito.edn_test.Bar,
                                                    :value {:a [1 2 3], :b {:c "Fooos"}}}
             (->> bar
                  pr-str
                  (read-string-safe {})))))))

(deftest double-roundtrip-test
  (testing "Test two roundtrips, one incognito and deserialize at end."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
      (is (= bar (->> bar
                      pr-str
                      (read-string-safe {})
                      pr-str
                      (read-string-safe {'incognito.edn_test.Bar map->Bar})))))))
