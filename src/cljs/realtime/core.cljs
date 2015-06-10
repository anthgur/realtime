(ns realtime.core
  (:require [realtime.comms :as comms]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [cljsjs.leaflet]
            [cljs.reader]))

(enable-console-print!)

(defonce client-id (cljs.core/random-uuid))

(defonce socket-dispatch (rf/dispatch [:open-websocket]))

(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:app/client-id client-id
     :app/ws nil
     :app/vehicles nil
     :leaflet/map-obj nil}))

(rf/reg-event-db :open-websocket comms/open-ws!)

(rf/reg-event-db
  :map-obj-initialized
  (fn [db [_ map-obj]]
    (assoc db :leaflet/map-obj map-obj)))

(rf/reg-event-db
  :vehicles/push
  (fn [db [_ new-vehicles]]
    (assoc db :app/vehicles new-vehicles)))

(rf/reg-sub
  :vehicles
  (fn [db _]
    (:app/vehicles db)))

(rf/reg-sub
  :leaflet-map-obj
  (fn [db _]
    (:leaflet/map-obj db)))

(def vehicle-id (comp :id :vehicle :vehicle))

(defn vehicle-ui
  [vehicle vehicle-layer-group]
  (let [marker (.circleMarker js/L)
        {:keys [:latitude :longitude]} (-> vehicle :vehicle :position)]
    (reagent/create-class
      {:component-did-mount
       (fn []
         (.addLayer vehicle-layer-group marker))
       :component-will-unmount
       (fn []
         (.removeLayer vehicle-layer-group marker))
       :reagent-render
       (fn [vehicle vehicle-layer-group]
         (doto marker
           (.setLatLng #js [latitude longitude])
           (.setRadius 5)
           (.bindPopup (pr-str vehicle)))
         nil)})))

(defonce tile-layer
  (.tileLayer
    js/L "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    (clj->js {:attribution
              "&copy; <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a>"
              :maxZoom 18})))

(def boston-coords #js [42.360 -71.054])

(defn vehicles-ui
  [vehicle-layer-group]
  (let [vehicles (rf/subscribe [:vehicles])]
    (fn [vehicle-layer-group]
      [:span (for [vehicle @vehicles]
               ^{:key (vehicle-id vehicle)}
               [vehicle-ui vehicle vehicle-layer-group])])))

(defn map-ui
  []
  (let [map-obj (rf/subscribe [:leaflet-map-obj])
        vehicle-layer-group (.layerGroup js/L)]
    (reagent/create-class
      {:component-did-mount
       (fn []
         (let [map-obj (.map js/L "map" #js {:center boston-coords :zoom 13})]
           (.addTo vehicle-layer-group map-obj)
           (.addTo tile-layer map-obj)
           (rf/dispatch [:map-obj-initialized map-obj])))
       :component-will-unmount
       (fn []
         (.remove @map-obj))
       :reagent-render
       (fn []
         [:div#map {:style {:height "400px"}}
          [vehicles-ui vehicle-layer-group]])})))

(defn ui
  []
  [map-ui])

;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])     ;; puts a value into application state
  (reagent/render [ui]              ;; mount the application's ui into '<div id="app" />'
                  (js/document.getElementById "app")))
