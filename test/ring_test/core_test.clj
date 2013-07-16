(ns ring-test.core-test
  (:require [clojure.test :refer :all]
            [ring-test.core :refer :all]
            [ring.mock.request :refer [request]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [response]]))

(defn handler [{params :params}]
  (response (str (params "s"))))

(def app
  (-> handler
      wrap-params))

(deftest returns-a-response-single
  (let [resp (run-ring-app app (request :get "/" {:s "hi"}))]
    (is (= "hi" (:body resp)))
    (is (= 200 (:status resp)))))

(deftest returns-a-response-multiple
  (let [resp (run-ring-app
               app
               (request :get "/" {:s "a"})
               (request :get "/" {:s "b"}))]
    (is (= "b" (:body resp)))
    (is (= 200 (:status resp)))))

