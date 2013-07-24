# ring-test

An integration test framework for Ring web applications. Currently supports sessions and cookies. Does not yet support file uploads. Inspired by Ruby's [Rack::Test](https://github.com/brynary/rack-test).

## Simple usage

The function `run-ring-app` accepts a Ring application and one or more requests (optionally constructed with [ring-mock](http://github.com/weavejester/ring-mock)) and returns the response map from the final request.  The session and cookies carry through all requests passed to a single call to `run-ring-app`.

```clojure
(ns ring-test.session-test
  (:require [clojure.test :refer :all]
            [ring-test.core :refer [run-ring-app]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.mock.request :refer [request]]))

(defn handler [{session :session}]
  (let [n (:n session 1)]
    {:body (str n) :session {:n (inc n)}}))

(def app (wrap-session handler))

(deftest session-carries-over
  (let [response
        (run-ring-app
          app
          (request :get "/")
          (request :get "/"))]
    (is (= "2" (:body response)))))
```

## Accessing the last response

In the example above only request maps are passed to `run-ring-app`.
If the next request depends on the previous request then pass a
function accepting a single argument: the previous response.  The
function should return a new request map.  For example:

```clojure
(defn handler [{params :params}]
  (response (str (params "s"))))

(def app (-> handler wrap-params))

(deftest doubles-previous-response
  (let [resp (run-ring-app
               app
               (request :get "/" {:s "b"}) ; Response body is "b".
               (fn [{body :body}]
                 (request :get "/" {:s (str body body)})))]
         (is (= "bb" (:body resp)))))
```

## Installation

Add the following to your project.clj:

```clojure
[ring-test "0.1.2"]
```

## License

Copyright © 2013 Russell Reas and released under an MIT license.

