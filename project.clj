(defproject io.replikativ/incognito "0.2.5"
  :description "Safe transport of unknown record types in distributed systems."
  :url "https://github.com/replikativ/incognito"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.439" :scope "provided"]
                 [org.clojure/data.fressian "0.2.1" :scope "provided"]
                 [com.cognitect/transit-clj "0.8.313" :scope "provided"]
                 [com.cognitect/transit-cljs "0.8.256":scope "provided"]
                 [fress "0.3.1":scope "provided" ]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :hooks [leiningen.cljsbuild]

  :profiles {:dev  {:dependencies [[com.cemerick/piggieback "0.2.1"]]
                    :figwheel     {:nrepl-port       7888
                                   :nrepl-middleware ["cider.nrepl/cider-middleware"
                                                      "cemerick.piggieback/wrap-cljs-repl"]}
                    :plugins      [[lein-figwheel "0.5.8"]]}}

  :source-paths ["src"]
  :test-paths ["test"]

  :cljsbuild
  {:test-commands {"unit-tests" ["node" "target/unit-tests.js"]}
   :builds
   {:tests
    {:source-paths ["src" "test"]
     :notify-command ["node" "target/unit-tests.js"]
     :compiler {:output-to "target/unit-tests.js"
                :optimizations :none
                :target :nodejs
                :main incognito.fressian-test}}}})
