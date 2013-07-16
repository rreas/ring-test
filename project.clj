(defproject ring-test "0.1.0"
  :description "An integration test framework for ring web applications."
  :url "http://github.com/rreas/ring-test"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.0"]
                 [ring-mock "0.1.5"]
                 [clj-time "0.5.1"]]
  :profiles {:dev {:dependencies [[compojure "1.1.5"]]}})

