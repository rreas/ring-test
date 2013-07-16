(ns ring-test.session-test
  (:require [clojure.test :refer :all]
            [ring-test.core :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.mock.request :refer [request]]))

(defn handler [{session :session :as r}]
  (let [n (:n session 1)]
    {:body (str n) :session {:n (inc n)}}))

(def app-memory (wrap-session handler))
(def app-cookie (wrap-session handler {:store (cookie-store {:key "0987654321012345"})}))
(def app-secure (wrap-session handler {:store (cookie-store {:key "abcdefghijklmnop" :secure true})}))

(defn session-carries-over [app]
  (let [resp
        (run-ring-app
          app
          (request :get "/")
          (request :get "/"))]
    (is (= "2" (:body resp)))))

(deftest session-carries-over-memory (session-carries-over app-memory))
(deftest session-carries-over-cookie (session-carries-over app-cookie))
(deftest session-carries-over-secure (session-carries-over app-secure))

(defn session-resets [app]
  (run-ring-app app (request :get "/"))
  (is (= "1" (:body (run-ring-app app (request :get "/"))))))

(deftest session-resets-memory (session-resets app-memory))
(deftest session-resets-cookie (session-resets app-cookie))
(deftest session-resets-secure (session-resets app-secure))

