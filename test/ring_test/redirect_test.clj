(ns ring-test.redirect-test
  (:use [compojure.core]
        [ring-test.helpers])
  (:require [clojure.test :refer :all]
            [ring.util.response :refer [redirect redirect-after-post response]]
            [ring-test.core :refer :all]
            [ring.mock.request :refer [request]]
            [compojure.handler :refer [site]]))
 
(defroutes test-app-routes
  (GET "/" {cookies :cookies :as r} (print-cookies cookies r))
  (GET "/redir-1" [] (into (redirect "/") {:cookies {"a" {:value "1"}}}))
  (GET "/redir-2" [] (into (redirect-after-post "/") {:cookies {"a" {:value "2"}}}))
  (GET "/forever" [] (redirect "/forever")))

(def app (site test-app-routes))

(deftest redirects-after-302
  (let [resp (run-ring-app app (request :get "/redir-1"))]
    (is (= "1" (:body resp)))))

(deftest redirects-after-303
  (let [resp (run-ring-app app (request :get "/redir-2"))]
    (is (= "2" (:body resp)))))

(deftest errors-after-5-redirects
  (is (thrown? RuntimeException
               (run-ring-app app (request :get "/forever")))))

