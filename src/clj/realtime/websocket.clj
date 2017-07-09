(ns realtime.websocket
  (:require [realtime.events :refer [Send send-event]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [bidi.bidi :as bidi]
            [org.httpkit.server :refer [send! with-channel on-close on-receive]]
            [taoensso.timbre :as timbre]))

(defrecord Client [id channel routes-sub])

(extend-protocol Send
  Client
  (send-event [client event data]
    (let [{:keys [id channel]} client
          message (pr-str [event data])]
      (timbre/info "sending event to client" {:client-id id :event event})
      (send! channel message))))

(defn websocket-handler
  [client-store last-message]
  (fn [{{:keys [client-id]} :params :as request}]
    (with-channel
      request ch
      (swap! client-store assoc client-id
             (Client. client-id ch nil))
      (on-close ch
        (fn [_] (swap! client-store dissoc client-id)))
      (on-receive ch
        (fn [data]
          (let [{{:keys [routes-sub]} :data :as event} (read-string data)
                client (@client-store client-id)]
            (timbre/info "client subscribing" {:client-id client-id})
            (swap! client-store update-in [client-id]
                   #(assoc % :routes-sub routes-sub))
            (send-event client :vehicles/push @last-message)))))))

(defrecord WebSocket [client-store last-message]
  bidi/RouteProvider
  (routes [_]
    ["" {"/websocket"
         (-> (websocket-handler client-store last-message)
             wrap-edn-params
             wrap-keyword-params
             wrap-params)}]))
