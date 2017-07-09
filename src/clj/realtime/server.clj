(ns realtime.server
  (:require [realtime.system :refer [system]]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn port
  []
  (read-string (or (System/getenv "PORT") "8080")))

(defn mbta-pb-url
  []
  (System/getenv "MBTA_PB_URL"))

(defn -main
  [& args]
  (component/start (system {:port (port)
                            :consume-gtfs? true
                            :mbta-pb-url (mbta-pb-url)})))
