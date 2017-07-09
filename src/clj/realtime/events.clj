(ns realtime.events)

(defprotocol Send
  (send-event [this event data]))
