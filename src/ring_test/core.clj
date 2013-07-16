(ns ring-test.core
  (:require [ring-test.cookies :refer [use-cookies]]))

(defn run-ring-app [app & requests]
  (:response (reduce (use-cookies app) {} requests)))

