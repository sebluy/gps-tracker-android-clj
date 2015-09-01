(ns android.sebluy.gpstracker.gps
  (:require [neko.activity :as activity]
            [neko.ui.mapping :as mapping]
            [neko.threading :as threading]
            [clojure.core.async :as async]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.widget TableLayout
                           TableRow]
           [com.google.android.gms.common.api GoogleApiClient$Builder
                                              GoogleApiClient$ConnectionCallbacks
                                              GoogleApiClient$OnConnectionFailedListener]
           [com.google.android.gms.location LocationServices
                                            LocationRequest
                                            LocationCallback]
           [android.view WindowManager$LayoutParams]
           (android.os Looper)))

(def attributes (atom nil))

(defn make-connection-callbacks [chan]
  (reify GoogleApiClient$ConnectionCallbacks
    (onConnected [_ _]
      (async/put! chan :connected))
    (onConnectionSuspended [_ _]
      (async/put! chan :disconnected))))

(defn make-on-connection-failed-listener [chan]
  (reify GoogleApiClient$OnConnectionFailedListener
    (onConnectionFailed [_ _]
      (async/put! chan :disconnected))))

(defn make-google-api-client [activity chan]
  (.. (GoogleApiClient$Builder. activity)
      (addConnectionCallbacks (make-connection-callbacks chan))
      (addOnConnectionFailedListener (make-on-connection-failed-listener chan))
      (addApi LocationServices/API)
      (build)))

(defn make-location-callback [control-chan location-chan]
  (proxy [LocationCallback] []
    (onLocationAvailability [availability]
      (if (.isLocationAvailable availability)
        (async/put! control-chan :tracking-started)
        (async/put! control-chan :tracking-stopped)))
    (onLocationResult [location-result]
      (async/put! location-chan (.getLastLocation location-result)))))

(defn make-location-request []
  (.. (LocationRequest.)
      (setInterval 1000)
      (setPriority LocationRequest/PRIORITY_HIGH_ACCURACY)))

(mapping/defelement
  :table-layout
  :classname TableLayout
  :inherits :view-group)

(mapping/defelement
  :table-row
  :classname TableRow
  :inherits :view)

(defn table-row [title value]
  [:table-row {}
   [:text-view {:text (str title)}]
   [:text-view {:text (str value)}]])

(defn tracking-table [attributes]
  (into [:table-layout {}]
        (map (partial apply table-row)
             attributes)))

(defn ui [activity]
  [:linear-layout {}
   (tracking-table @attributes)
   [:button {:text "Pause"}]
   [:button {:text     "Finish"
             :on-click (fn [_] (.finish activity))}]])

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view! activity (ui activity))))

(defn start-location-updates [google-api-client location-listener]
  (.requestLocationUpdates LocationServices/FusedLocationApi
                           google-api-client
                           (make-location-request)
                           location-listener
                           (Looper/getMainLooper)))

(defn stop-location-updates [google-api-client location-listener]
  (.removeLocationUpdates LocationServices/FusedLocationApi
                          google-api-client
                          location-listener))

(defn location->map [location]
  (merge {:latitude  (.getLatitude location)
          :longitude (.getLongitude location)}
         (if (.hasSpeed location)
           {:speed (.getSpeed location)})
         (if (.hasAccuracy location)
           {:accuracy (.getAccuracy location)})))

; TODO: replace location listener with listener callback
(defn run-location-machine [activity]
  (let [status-chan (async/chan)
        control-chan (async/chan)
        location-chan (async/chan)
        location-callback (make-location-callback control-chan location-chan)
        google-api-client (make-google-api-client activity control-chan)]
    (async/go
      (loop [state :disconnected]
        (async/put! status-chan state)
        (condp = [state (async/<! control-chan)]
          [:disconnected :start]
          (do (.connect google-api-client)
              (recur :connecting))
          [:connecting :connected]
          (do (start-location-updates google-api-client location-callback)
              (recur :connected))
          [:connecting :disconnected]
          (recur :disconnected)
          [:connected :tracking-started]
          (recur :tracking)
          [:connected :stop]
          (do (.disconnect google-api-client)
              (recur :disconnected))
          [:connected :disconnected]
          (recur :disconnected)
          [:tracking :stop]
          (do (stop-location-updates google-api-client location-callback)
              (.disconnect google-api-client)
              (recur :disconnected))
          (recur state))))
    (async/go
      (loop []
        (swap! attributes assoc :status (async/<! status-chan))
        (render-ui activity)
        (recur)))
    (async/go
      (loop []
        (let [location (async/<! location-chan)]
          (swap! state/state update :path #(conj % (location->map location)))
          (swap! attributes assoc
                 :latitude (.getLatitude location)
                 :longitude (.getLongitude location)
                 :speed (.getSpeed location)
                 :accuracy (.getAccuracy location)))
        (render-ui activity)
        (recur)))
    [status-chan control-chan location-chan]))

(defn keep-screen-on [activity boolean]
  (threading/on-ui
    (let [window (.getWindow activity)]
      (if boolean
        (.addFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)
        (.clearFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)))))

(defn get-chan [activity key]
  (get-in @(activity/get-state activity) [:channels key]))

(activity/defactivity
  android.sebluy.gpstracker.TrackingActivity
  :key :tracking
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (swap! state/state assoc :path [])
    (reset! attributes {:status :pending})
    (let [[status-chan control-chan location-chan] (run-location-machine this)]
      (reset! (activity/get-state this)
              {:channels {:status status-chan :control control-chan :location location-chan}}))
    (render-ui this))
  (onStart
    [this]
    (.superOnStart this)
    (async/put! (get-chan this :control) :start)
    (keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (async/put! (get-chan this :control) :stop)
    (keep-screen-on this false)))

