# incognito

Different Clojure(Script) serialization protocols like `edn`, `fressian` or
`transit` offer different ways to serialize custom types. In general
they fall back to maps for unknown record types, which is a reasonable
default in many situations. But when you build a distributed data
management system parts of your system might not care about the record
types while others do. This library safely wraps unknown record types
and therefore allows to unwrap them later. It also unifies record
serialization between `fressian` and `transit` as long as you can
express your serialization format in Clojure's default datastructures.

## Usage

Add this to your project dependencies:
[![Clojars Project](http://clojars.org/io.replikativ/incognito/latest-version.svg)](http://clojars.org/io.replikativ/incognito)

Exclude all serialization libraries you don't need, e.g. for edn support only:
```clojure
[io.replikativ/incognito "0.1.0" :exclusions [org.clojure/data.fressian com.cognitect/transit-clj]]
```

In general you can control serialization by `write-handlers` and `read-handlers`:

```clojure
(defrecord Bar [a b])

(def write-handlers {'user.Bar (fn [bar] (assoc bar :c "banana"))})
(def read-handlers {'user.Bar map->Bar})
```
*NOTE*: The syntax quote for the handlers which ensures that you
can deserialize unknown classes.

A write-handler has to return an associative datastructure which is
internally stored as an untyped map together with the tag information.

Extracted from the tests:

### edn

```clojure
(require '[incognito.edn :refer [read-string-safe]])

(let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
  (= bar (->> bar
              pr-str
              (read-string-safe {})
              pr-str
              (read-string-safe read-handlers))))
```

For dashed namespace names you need a custom printer to be
ClojureScript conform.

```clojure
(defmethod print-method some_namespace.Bar [v ^java.io.Writer w]
  (.write w (str "#some-namespace.Bar" (into {} v))))
```


### transit
```clojure
(require '[incognito.transit :refer [incognito-write-handler incognito-read-handler]]
         '[cognitect.transit :as transit])

(let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
  (= (assoc bar :c "banana")
     (with-open [baos (ByteArrayOutputStream.)]
       (let [writer (transit/writer baos :json
                                    {:handlers {java.util.Map
                                                (incognito-write-handler
                                                 (atom write-handlers))}})]
         (transit/write writer bar)
         (let [bais (ByteArrayInputStream. (.toByteArray baos))
               reader (transit/reader bais :json
                                      {:handlers {"incognito"
                                                  (incognito-read-handler (atom read-handlers))}})]
           (transit/read reader))))))
```

### fressian

```clojure
(require '[clojure.data.fressian :as fress]
         '[incognito.fressian :refer [incognito-read-handlers
                                      incognito-write-handlers]])

(let [bar (map->Bar {:a [1 2 3] :b {:c "Fooos"}})]
  (= (assoc bar :c "banana")
     (with-open [baos (ByteArrayOutputStream.)]
       (let [w (fress/create-writer baos
                                    :handlers
                                    (-> (merge fress/clojure-write-handlers
                                               (incognito-write-handlers (atom write-handlers)))
                                        fress/associative-lookup
                                        fress/inheritance-lookup))] ;
         (fress/write-object w bar)
         (let [bais (ByteArrayInputStream. (.toByteArray baos))]
           (fress/read bais
                       :handlers
                       (-> (merge fress/clojure-read-handlers
                                  (incognito-read-handlers (atom read-handlers)))
                           fress/associative-lookup)))))))
```

## TODO
- cross-platform transit code

## License

Copyright © 2015 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
