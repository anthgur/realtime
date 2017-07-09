(ns realtime.system
  (:require [realtime.gtfs :as gtfs]
            [realtime.websocket :as websocket]
            [clojure.core.async :refer [chan go-loop <! close!] :as async]
            [com.stuartsierra.component :as component]
            [bidi.bidi :as bidi]
            [bidi.ring :as b.ring]
            [ring.middleware.edn :as r.m.edn]
            [ring.middleware.defaults :as r.m.defaults]
            [ring.util.response :as r.u.response]
            [modular.http-kit :as m.http-kit]
            [modular.ring :as m.ring]
            [modular.bidi :as m.bidi]
            [taoensso.timbre :as timbre]))

(defn system
  [{:keys [port consume-gtfs? mbta-pb-url]}]
  (component/system-using
   (component/system-map
    :gtfs-feed
    (component/using
     (gtfs/map->GTFSFeed {:timeout 10000
                          :url mbta-pb-url
                          :running? consume-gtfs?})
     {:out-chan :gtfs-out-chan})

    :gtfs-out-chan
    (chan (async/sliding-buffer 5))

    :feed-pusher
    (component/using
     (gtfs/map->FeedPusher {})
     {:in-chan :gtfs-out-chan
      :client-store :client-store-atom
      :last-message :last-message-atom})

    :middleware
    #(r.m.defaults/wrap-defaults % r.m.defaults/site-defaults)

    :router
    (m.bidi/new-router)
    
    :router-head
    (component/using
     (m.ring/new-web-request-handler-head)
     {:request-handler :router
      :middleware      :middleware})

    :http-server
    (component/using
     (m.http-kit/new-webserver :port port)
     [:router-head :middleware])

    :frontend
    (reify bidi/RouteProvider
      (routes [_]
        ["" {"/"
             (fn [_]
               (-> (r.u.response/resource-response "index.html")
                   (r.u.response/header "Content-Type" "text/html")))}]))

    :websocket
    (component/using
     (websocket/map->WebSocket {})
     {:client-store :client-store-atom
      :last-message :last-message-atom})

    :client-store-atom
    (atom {})

    :last-message-atom
    (atom nil))
   
   {:router [:frontend :websocket]}))
