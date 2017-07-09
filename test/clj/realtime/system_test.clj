(ns realtime.system-test
  (:require [realtime.system :refer [system]]
            [com.stuartsierra.component :refer [start stop]]
            [org.httpkit.client :as http])
  (:use clojure.test))

(defn route
  [{:keys [port]} path]
  (str "http://localhost:" port path))

(deftest test-webpage
  (testing "server provides an index page"
    (let [config {:port 8080
                  :consume-gtfs? false
                  :mbta-pb-url "irrelevant"}
          sys (system config)]
      (start sys)
      (let [{:keys [status error body]} @(http/get (route config "/"))]
        (is (nil? error))
        (is (= 200 status))
        (is (string? body)))
      (stop sys))))
