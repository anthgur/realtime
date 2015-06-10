(ns realtime.gtfs
  (:require [clojure.core.async :refer [<! >! <!! go go-loop chan close!] :as async]
            [flatland.protobuf.core :as f.p.core]
            [com.stuartsierra.component :as component])
  (:import [com.google.transit.realtime GtfsRealtime$FeedMessage]))

(def vehicles-url (System/getenv "MBTA_PB_URL"))

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
    (let [running? (or running? (atom true))]
      (go-loop []
        (when-let [message (and @running? (feed-message url))]
          (>! out-chan (protomap message))
          (<! (async/timeout timeout))
          (recur)))
      (assoc component :running? running?)))

  (stop [_]
    (when @running?
      (reset! running? false)
      (close! out-chan))))

(comment
  (clojure.pprint/pprint (f.p.core/protobuf-schema FeedMessage))

  (def feed
    (-> {:timeout 5000 :out-chan (chan (async/sliding-buffer 5)) :url vehicles-url}
        map->GTFSFeed
        component/start))

  (<!! (:out-chan feed))

  (component/stop feed))
