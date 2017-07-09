(ns realtime.gtfs
  (:require [clojure.core.async :refer [<! >! <!! go go-loop chan close!] :as async]
            [flatland.protobuf.core :as f.p.core]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre])
  (:import [com.google.transit.realtime GtfsRealtime$FeedMessage]
           (java.util UUID)))

(def FeedMessage (f.p.core/protodef GtfsRealtime$FeedMessage))

(defn protomap
  [message]
  (->> message
       (.toByteArray)
       (f.p.core/protobuf-load FeedMessage)))

(defn feed-message
  [url]
  (GtfsRealtime$FeedMessage/parseFrom
   (.openStream (java.net.URL. url))))

(defrecord GTFSFeed [timeout out-chan url running?]
  component/Lifecycle
  (start [component]
    (let [component (assoc component
                      :running? (atom running?)
                      :uuid (UUID/randomUUID))]
      (timbre/info "starting gtfs feed reader" component)
      (go-loop []
        (let [message (and @(:running? component)
                           (feed-message url))
              uuid (UUID/randomUUID)]
          (when message
            (timbre/info "reading gtfs feed message"
                         {:component component :uuid uuid})
            (let [data (protomap message)]
              (timbre/info "read gtfs feed message"
                           {:component component :uuid uuid})
              (>! out-chan data))
            (timbre/info "gtfs feed reader sleeping" component)
            (<! (async/timeout timeout))
            (timbre/info "gtfs feed reader waking up" component)
            (recur))))
      component))

  (stop [component]
    (let [{:keys [running?]} component]
      (when (and running? @running?)
        (timbre/info "gtfs feed reader stopping")
        (reset! running? false)
        (close! out-chan)
        (dissoc component :running?)))))
