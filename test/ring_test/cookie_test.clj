(ns ring-test.cookie-test
  (:use [compojure.core])
  (:require [clojure.test :refer :all]
            [ring-test.core :refer :all]
            [ring-test.cookies :refer [cookie-uri-path]]
            [ring.mock.request :refer [request]]
            [clj-time.core :refer [now minus days]]
            [compojure.handler :refer [site]]))

(defn set-cookie [value]
  {:cookies {"a" {:value value :path "/"}}})

(defn set-cookies [a b]
  {:cookies {"a" {:value a :path "/"} "b" {:value b :path "/"}}})

(defn set-expired-cookie []
  {:cookies {"a" {:value 1 :expires (minus (now) (days 1)) :path "/"}}})

(defn set-secure-cookie [value]
  {:cookies {"a" {:value value :secure true :path "/"}}})

(defn set-cookie-with-path [id value path]
  {:cookies {id {:value value :path path}}})

(defn set-cookie-without-path [id value]
  {:cookies {id {:value value}}})

(defn set-cookie-with-domain [value domain]
  {:cookies {"a" {:value value :domain domain :path "/"}}})

(defn print-cookies [cookies r]
  {:body (apply str (map #(get-in cookies [% :value]) (sort (keys cookies))))
   :status 200})

(defroutes test-app-routes
  (GET "/" [] {})
  (GET "/cookies/set" [value] (set-cookie value))
  (GET "/cookies/set-many" [a b] (set-cookies a b))
  (GET "/cookies/set-expired" [] (set-expired-cookie))
  (GET "/cookies/set-secure" [value] (set-secure-cookie value))
  (GET "/cookies/with-path" [id value path] (set-cookie-with-path id value path))
  (GET "/cookies/path/1" [id value] (set-cookie-without-path id value))
  (GET "/cookies/path/1/2" [id value] (set-cookie-without-path id value))
  (GET "/cookies/with-domain" [value domain] (set-cookie-with-domain value domain))
  (GET "/cookies/path" {cookies :cookies :as r} (print-cookies cookies r))
  (GET "/cookies/show" {cookies :cookies :as r} (print-cookies cookies r)))

(def app (site test-app-routes))

(defn expect-response
  [expected & reqs]
  (let [resp (apply run-ring-app app reqs)]
    (is (= expected (:body resp)))))

(deftest keeps-a-cookie-jar-single
  (expect-response
    "1"
    (request :get "/cookies/set" {:value 1})
    (request :get "/cookies/show")))

(deftest keeps-a-cookie-jar-multiple
  (expect-response
    "2"
    (request :get "/cookies/set" {:value 1})
    (request :get "/cookies/set" {:value 2})
    (request :get "/cookies/show")))

(deftest sets-multiple-cookies
  (expect-response
    "12"
    (request :get "/cookies/set-many" {:a 1 :b 2})
    (request :get "/cookies/show")))

(deftest does-not-send-expired-cookies
  (expect-response
    ""
    (request :get "/cookies/set-expired")
    (request :get "/cookies/show")))

(deftest persists-over-nil-header-cookie-requests
  (expect-response
    "1"
    (request :get "/cookies/set" {:value 1})
    (request :get "/")
    (request :get "/cookies/show")))

(deftest strips-secure-cookies-on-http
  (expect-response
    ""
    (request :get "https://example.com/cookies/set-secure" {:value 1})
    (request :get "http://example.com/cookies/show")))

(deftest keeps-secure-cookies-on-https
  (expect-response
    "1"
    (request :get "https://example.com/cookies/set-secure" {:value 1})
    (request :get "https://example.com/cookies/show")))

(deftest strips-by-path-when-provided
  (expect-response
    "1"
    (request :get "/cookies/with-path" {:id "a" :value 1 :path "/cookies/show"})
    (request :get "/cookies/with-path" {:id "b" :value 2 :path "/hidden"})
    (request :get "/cookies/show")))

(deftest extract-uri-path
  (is (= "/cookies" (cookie-uri-path "/cookies/show")))
  (is (= "/cookies/show/deep" (cookie-uri-path "/cookies/show/deep/")))
  (is (= "/" (cookie-uri-path "/cookies")))
  (is (= "/" (cookie-uri-path "/")))
  (is (= "/" (cookie-uri-path ""))))

(deftest uses-verbatim-path-when-provided
  (expect-response
    "2"
    (request :get "/cookies/with-path" {:id "a" :value 1 :path "/cookies/show/fail"})
    (request :get "/cookies/with-path" {:id "b" :value 2 :path "/cookies/show"})
    (request :get "/cookies/show")))

(deftest sets-default-path-to-directory
  (expect-response
    "1"
    (request :get "/cookies/path/1" {:id "a" :value 1})
    (request :get "/cookies/path/1/2" {:id "b" :value 2})
    (request :get "/cookies/path")))

(deftest removes-mismatching-domain-cookies
  (expect-response
    ""
    (request :get "http://www.a.com/cookies/set" {:value 1})
    (request :get "http://qqq.a.com/cookies/show")))

;FIXME: Setting cookies on www.foo.com from www.bar.com will not work for security reasons.

(deftest disallows-subdomains-for-top-level
  (expect-response
    ""
    (request :get "http://a.com/cookies/set" {:value 1})
    (request :get "http://www.a.com/cookies/show")))

(deftest allows-on-subdomains-if-requested
  (expect-response
    "1"
    (request :get "http://a.com/cookies/with-domain" {:value 1 :domain ".a.com"})
    (request :get "http://www.a.com/cookies/show")))

(deftest tracks-cookies-by-domain
  (expect-response
    "1"
    (request :get "http://a.com/cookies/set" {:value 1})
    (request :get "http://b.com/cookies/set" {:value 2})
    (request :get "http://a.com/cookies/show")))

(deftest prefers-cookies-with-subdomain-match
  (expect-response
    "2"
    (request :get "http://a.com/cookies/with-domain" {:value 1 :domain ".a.com"})
    (request :get "http://www.a.com/cookies/set" {:value 2})
    (request :get "http://www.a.com/cookies/show")))

