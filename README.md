# ring-test

An integration test framework for Ring web applications. Currently supports sessions and cookies. Does not yet support file uploads. Inspired by Ruby's [Rack::Test](https://github.com/brynary/rack-test).

## Usage

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

## Installation

Add the following to your project.clj:

```clojure
[ring-test "0.1.1"]
```

## License

Copyright © 2013 Russell Reas and released under an MIT license.

