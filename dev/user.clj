(ns user
  (:require
   [realtime.system :as system]
   [reloaded.repl :as repl]))

(defn dev
  []
  (constantly
   (system/system {:port 8080})))

(repl/set-init! (dev))
