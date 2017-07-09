(ns realtime.subscriptions
  (:require [taoensso.timbre :as timbre]
            [org.httpkit.server :as o.h.server]))

(defn send!
  [channel event data]
  (let [message (pr-str [event data])]
    (o.h.server/send! channel message)))

(defn send-sub!
  [{:keys [client-id channel]} message]
  (timbre/info "sending client subscription" {:client-id client-id})
  (send! channel :vehicles/push message))
