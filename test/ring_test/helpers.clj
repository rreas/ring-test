(ns ring-test.helpers)

(defn print-cookies [cookies r]
  {:body (apply str (map #(get-in cookies [% :value]) (sort (keys cookies))))
   :status 200})

