(ns realtime.websocket
  (:require [realtime.event-dispatch :refer [dispatch!]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [bidi.bidi :as bidi]
            [org.httpkit.server :refer [with-channel on-close on-receive]]))


(defn websocket-handler
  [client-store last-message]
  (fn [{{:keys [client-id]} :params :as request}]
    (with-channel
      request ch
      (swap! client-store assoc client-id
             {:client-id client-id
              :channel ch})
      (on-close ch
        (fn [_] (swap! client-store dissoc client-id)))
      (on-receive ch
        (fn [data]
          (->> data read-string
               (dispatch! {:client-store client-store
                           :last-message last-message}
                          (@client-store client-id))))))))

(defrecord WebSocket [client-store last-message]
  bidi/RouteProvider
  (routes [_]
    ["" {"/websocket"
         (-> (websocket-handler client-store last-message)
             wrap-edn-params
             wrap-keyword-params
             wrap-params)}]))
