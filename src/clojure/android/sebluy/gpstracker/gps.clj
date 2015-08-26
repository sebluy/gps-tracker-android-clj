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

#_(defn run-location-machine [activity]
  (let [status-chan (async/chan)
        control-chan (async/chan)
        google-api-client (make-google-api-client activity status-chan)]
    (async/go
      (while (not= :start (async/<! control-chan)))
      (.connect google-api-client)
      (while (not= :stop (async/<! control-chan)))
      (.disconnect google-api-client))
    [status-chan control-chan]))

(def status-chan (async/chan))
(def api-client (make-google-api-client (neko.debug/*a) status-chan))

#_(def chan-v (run-location-machine (neko.debug/*a)))

#_(neko.threading/on-ui (neko.notify/toast (name :connected)))

#_(async/go
  (neko.log/d (async/<! status-chan)))

#_(.connect api-client)

#_(async/put! (second chan-v) :start)

(mapping/defelement
  :table-layout
  :classname TableLayout
  :inherits :view-group)

(mapping/defelement
  :table-row
  :classname TableRow
  :inherits :view)

(defn table-row
  ([title] (table-row title "Pending"))
  ([title value]
   [:table-row {}
    [:text-view {:text title}]
    [:text-view {:text value}]]))

(defn tracking-table []
  (into [:table-layout {}]
        (map table-row
             ["Status"
              "Current Speed"
              "Average Speed"
              "Total Distance"
              "Duration"])))

(defn ui [activity]
  [:linear-layout {}
   (tracking-table)
   [:button {:text "Pause"}]
   [:button {:text     "Finish"
             :on-click (fn [_] (.finish activity))}]])

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view! activity (ui activity))))

(defn keep-screen-on [activity boolean]
  (threading/on-ui
    (let [window (.getWindow activity)]
      (if boolean
        (.addFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)
        (.clearFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)))))

(activity/defactivity
  android.sebluy.gpstracker.TrackingActivity
  :key :tracking
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this))
  (onStart
    [this]
    (.superOnStart this)
    (keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (keep-screen-on this false)))

