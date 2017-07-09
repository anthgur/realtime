(ns realtime.server
  (:require [realtime.system :refer [system]]
            [system.repl :refer [set-init! start]]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn port
  []
  (read-string (or (System/getenv "PORT") "8080")))

(defn -main
  [& args]
  (component/start (system {:port (port)})))
