(ns realtime.comms
  (:require [re-frame.core :as rf]
            [cljs.reader]))

(defn open-ws!
  [{:keys [client-id] :as state} _]
  (let [location (aget js/window "location" "origin")
        origin (second (.split location "//"))
        uuid-str (-> (.split
                       (pr-str client-id) "\"")
                     second)
        socket (js/WebSocket. (str "wss://" origin "/websocket?client-id=" uuid-str))]
    (aset socket "onopen"
          (fn [e]
            (println "sending subscribe")
            (.send
              socket
              {:event :routes/subscribe!
               :data {:routes-sub :all}})))
    (aset socket "onmessage"
          (fn [m]
            (let [[event data]
                  (cljs.reader/read-string (.-data m))]
              (println event "\n" (js/Date.))
              (rf/dispatch [event data]))))
    (assoc state :app/ws socket)))
