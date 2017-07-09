(ns realtime.system
  (:require [realtime.gtfs :as gtfs]
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
            [taoensso.timbre :as timbre]
            [org.httpkit.server :as o.h.server :refer [on-close on-receive]]))

(defn dedupe-ident
  [ident-fn]
  (fn [coll]
    (first (reduce
            (fn [[coll ids :as acc] item]
              (let [id (ident-fn item)]
                (if (contains? ids id)
                  acc [(conj coll item) (conj ids id)])))
            [[] #{}]
            coll))))

(defn vehicle-id
  [entity]
  (-> entity :vehicle :vehicle :id))

(def dedupe-message (dedupe-ident vehicle-id))

(defn send!
  [channel event data]
  (let [message (pr-str [event data])]
    (o.h.server/send! channel message)))

(defn send-sub!
  [{:keys [client-id channel]} message]
  (timbre/info "sending client subscription" {:client-id client-id})
  (send! channel :vehicles/push message))

(defmulti dispatch!
  (fn [_ _ {:keys [event]}]
    event))

(defmethod dispatch! :routes/subscribe!
  [{:keys [client-store last-message]}
   {:keys [client-id channel] :as subscription}
   {{:keys [routes-sub]} :data :as event}]
  (timbre/info "client subscribing" {:client-id client-id})
  (swap! client-store update-in
         [client-id]
         (fn [old] (assoc old :routes-sub routes-sub))) ;; sub currently ignored
  (send-sub! subscription @last-message))

(defmethod dispatch! :default
  [_ {:keys [channel]} message]
  (send! channel :unknown/event message))

(defn websocket-handler
  [client-store last-message]
  (fn [{{:keys [client-id]} :params :as request}]
    (o.h.server/with-channel
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
             r.m.edn/wrap-edn-params
             ring.middleware.keyword-params/wrap-keyword-params
             ring.middleware.params/wrap-params)}]))

(defrecord FeedPusher [client-store last-message in-chan]
  component/Lifecycle
  (start [component]
    (go-loop []
      (when-let [{:keys [entity] :as message} (<! in-chan)]
        (let [deduped (dedupe-message entity)]
          (reset! last-message deduped)
          (doseq [[_ {:keys [channel routes-sub]}]
                  @client-store]
            (when routes-sub
              (send-sub! channel deduped)))
          (recur))))
    component)

  (stop [component] component))

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
     (map->FeedPusher {})
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
     (map->WebSocket {})
     {:client-store :client-store-atom
      :last-message :last-message-atom})

    :client-store-atom
    (atom {})

    :last-message-atom
    (atom nil))
   
   {:router [:frontend :websocket]}))
