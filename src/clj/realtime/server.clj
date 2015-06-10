(ns realtime.server
  (:require [realtime.system :as system]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn -main
  [& args]
  (println "server starting")
  (component/start (system/system {:port (read-string (System/getenv "PORT"))}))
  (println "server started"))
