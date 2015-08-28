(ns android.sebluy.gpstracker.gps
  (:require [neko.activity :as activity]
            [neko.ui.mapping :as mapping]
            [neko.threading :as threading]
            [clojure.core.async :as async])
  (:import [android.widget TableLayout
                           TableRow]
           [com.google.android.gms.common.api GoogleApiClient$Builder
                                              GoogleApiClient$ConnectionCallbacks
                                              GoogleApiClient$OnConnectionFailedListener]
           [com.google.android.gms.location LocationServices]
           [android.view WindowManager$LayoutParams]))

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

(defn run-location-machine [activity]
  (let [status-chan (async/chan)
        control-chan (async/chan)
        google-api-client (make-google-api-client activity status-chan)]
    (async/go
      (loop []
        (while (not= :start (async/<! control-chan)))
        (.connect google-api-client)
        (while (not= :stop (async/<! control-chan)))
        (.disconnect google-api-client)
        (async/>! status-chan :disconnected)
        (recur)))
    (async/go
      (loop []
        (swap! attributes assoc :status (async/<! status-chan))
        (render-ui activity)
        (recur)))
    [status-chan control-chan]))

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
    (reset! attributes {:status :pending})
    (let [[status-chan control-chan] (run-location-machine this)]
      (reset! (activity/get-state this) {:channels {:status status-chan :control control-chan}}))
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

