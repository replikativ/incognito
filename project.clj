(defproject io.replikativ/incognito "0.2.2"
  :description "Safe transport of unknown record types in distributed systems."
  :url "https://github.com/replikativ/incognito"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [org.clojure/data.fressian "0.2.1" :scope "provided"]
                 [com.cognitect/transit-clj "0.8.285" :scope "provided"]

                 [com.cognitect/transit-cljs "0.8.239" :scope "provided"]]

  :profiles {:test {:dependencies [[clj-time "0.13.0"]]}})
