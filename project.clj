(defproject ring-test "0.1.3"
  :description "An integration test framework for Ring web applications."
  :url "http://github.com/rreas/ring-test"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.0"]
                 [ring-mock "0.1.5"]
                 [clj-time "0.6.0"]]
  :profiles {:dev {:dependencies [[compojure "1.1.5"]]}})

