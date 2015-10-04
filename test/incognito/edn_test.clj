(ns incognito.edn-test
  (:require [clojure.test :refer :all]
            [incognito.edn :refer :all]))

(defrecord Bar [a b])

(defmethod print-method Bar [v ^java.io.Writer w]
  (.write w (str "#incognito.edn-test.Bar" (into {} v))))

(deftest incognito-roundtrip-test
  (testing "Test incognito transport."
    (let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
      (is (= #incognito.base.IncognitoTaggedLiteral{:tag incognito.edn-test.Bar,
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
                      (read-string-safe {'incognito.edn-test.Bar map->Bar})))))))
