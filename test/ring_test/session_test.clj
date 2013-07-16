(ns ring-test.session-test
  (:require [clojure.test :refer :all]
            [ring-test.core :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.mock.request :refer [request]]))

(defn handler [{session :session}]
  (let [n (:n session 1)]
    {:body (str n) :session {:n (inc n)}}))

(def app-memory (wrap-session handler))
(def app-cookie (wrap-session handler {:store (cookie-store {:key "0987654321012345"})}))

(deftest session-carries-over-memory
  (let [resp
        (run-ring-app
          app-memory
          (request :get "/")
          (request :get "/"))]
    (is (= "2" (:body resp)))))

(deftest session-carries-over-cookie
  (let [resp
        (run-ring-app
          app-cookie
          (request :get "/")
          (request :get "/"))]
    (is (= "2" (:body resp)))))

(deftest session-gets-reset-memory
  (run-ring-app app-memory (request :get "/"))
  (is (= "1" (:body (run-ring-app app-memory (request :get "/"))))))

(deftest session-gets-reset-cookie
  (run-ring-app app-cookie (request :get "/"))
  (is (= "1" (:body (run-ring-app app-cookie (request :get "/"))))))

