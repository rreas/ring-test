(ns ring-test.cookies
  (:require [clojure.string :refer [split]]
            [clj-time.core :refer [now after?]]
            [clj-time.format :refer [parse]]))

(def cookie-keys {"Expires" :expires
                  "Domain" :domain
                  "Path" :path
                  "Secure" :secure
                  "HttpOnly" :http-only})

(defn cookie-uri-path
  "Attempts to match proper cookie path behavior.
   See item #4: http://tools.ietf.org/html/rfc6265#section-5.1.4"
  [uri]
  (let [path (last (re-find #"(.*)\/" uri))]
    (if (> (count (str path)) 0) path "/")))

(defn cookie-tld
  [domain]
  (re-find #"localhost|[\w]+\.[\w]+$" domain))

(defn- secure?
  [request [cookie-name cookie]]
  (if (and (= :http (:scheme request)) (:secure cookie)) false true))

(defn- valid-path?
  [request [cookie-name cookie]]
  (.startsWith (:uri request) (:path cookie)))

(defn- valid-domain?
  [request [cookie-name cookie]]
  (let [rd (:server-name request)
        cd (:domain cookie)]
    (or (= rd cd) (and (.startsWith cd ".") (= (cookie-tld rd) (subs cd 1))))))
 
(defn- not-expired?
  [[cookie-name cookie]]
  (if-let [e (:expires cookie)]
    (after? (if (string? e) (parse e) e) (now))
    true))

(defn- cookies-for-domain
  [request jar]
  (let [base-domain (:server-name request)]
    (merge (jar (str "." (cookie-tld base-domain))) (jar base-domain))))

(defn- cookie-string
  [cookies]
  (apply str (interpose "; " (map (fn [[k v]] (str k "=" (:value v))) cookies))))

(defn- cookie-value
  "Returns cookie with value or true for special cases."
  [k v]
  (case k :secure true :http-only true v))

(defn- replace-cookie-keys
  [attr]
  (let [[k v] (split attr #"=")]
    (if-let [t (cookie-keys k)]
      [t (cookie-value t v)]
      {:name k :value v})))

(defn- transform-cookie
  "Takes a Set-Cookie header and extracts information.
   Sets the default path and domain when neither are provided."
  [rq cookie]
  (merge
    {:path (cookie-uri-path (:uri rq)) :domain (:server-name rq)}
    (into {} (map replace-cookie-keys (split cookie #";")))))

(defn- read-cookies
  "Transforms a cookie into a map for processing."
  [rq rs]
  (let [cookies (get-in rs [:headers "Set-Cookie"])]
    (map (partial transform-cookie rq) cookies)))

(defn- update-cookie-jar
  "Updates the cookie jar with new cookies keyed by domain."
  [jar cookie]
  (assoc-in jar [(:domain cookie) (:name cookie)] cookie))

(defn- update-cookies
  "Extracts and keeps new cookies from current request."
  [jar rq rs]
  (reduce update-cookie-jar jar (read-cookies rq rs)))

(defn- keep-valid-cookies
  "Filters out invalid cookies for various reasons.
   Combines cookies for valid domain selections."
  [jar request]
  (->> (cookies-for-domain request jar)
       (filter (partial secure? request))
       (filter (partial valid-path? request))
       (filter not-expired?)))

(defn- update-request
  "Adds cookies to the request header when applicable."
  [jar request]
  (if jar
    (assoc-in request
              [:headers "cookie"]
              (cookie-string (keep-valid-cookies jar request)))
    request))

(defn- update-state
  "Tracks cookies and last request/response through app."
  [state rq rs]
  {:jar (update-cookies (:jar state) rq rs) :request rq :response rs})

(defn- with-cookies 
  "Accepts cookies and applies them to the next request.
   Returns the updated cookie jar after the request along
   with the last request and last response."
  [app state request]
  (let [request-with-cookies (update-request (:jar state) request)]
    (->> (app request-with-cookies)
         (update-state state request-with-cookies))))

(defn use-cookies [app]
  (partial with-cookies app))
 
