(ns android.sebluy.gpstracker.receive
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [android.sebluy.gpstracker.util :as util]
            [android.sebluy.gpstracker.bluetooth-scanner :as scanner]
            [neko.find-view :as find-view]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.content Context Intent]
           [android.bluetooth BluetoothAdapter]
           [android.app Activity]
           [android R$layout]
           [android.widget ArrayAdapter
                           AdapterView$OnItemClickListener]))

(defn make-list-click-listener [activity]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _])))

(defn fill-device-list [devices]
  (fn [activity]
    (let [[list-view] (find-view/find-views activity ::list-view)]
      (.setAdapter list-view (ArrayAdapter. activity R$layout/simple_list_item_1
                                            (or (keys devices) ["No devices"])))
      (.setOnItemClickListener list-view (make-list-click-listener activity)))))

(declare stop-scan start-scan)

(defn scanning-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text "Stop"
             :on-click (fn [_] (stop-scan activity))}]])

(defn idle-scan-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text "Start"
             :on-click (fn [_] (start-scan activity))}]])

(defn loading-ui []
  [:linear-layout {:orientation :vertical}
   [:text-view {:text "Loading..."}]
   [:progress-bar {}]])

(defn summary-ui []
  [:linear-layout {}
   [:text-view {:text "Success"}]])

(defn render-ui [activity ui post-render-fn]
  (threading/on-ui
    (activity/set-content-view! activity ui)
    (post-render-fn activity)))

(defn device-key [device]
  (or (.getName device) (.getAddress device)))

(defn add-device [activity device]
  (let [key (device-key device)]
    (swap! state/state assoc-in [:devices key] device)
    (render-ui activity (scanning-ui activity) (fill-device-list (@state/state :devices)))))

(defn get-bluetooth-adapter [activity]
  (.. activity (getSystemService Context/BLUETOOTH_SERVICE) getAdapter))

(defn bluetooth-enabled? [activity]
  (let [bluetooth-adapter (get-bluetooth-adapter activity)]
    (and (some? bluetooth-adapter) (.isEnabled bluetooth-adapter))))

(defn get-bonded-devices [adapter]
  (reduce (fn [devices device]
            (assoc devices (device-key device) device))
          {} (.getBondedDevices adapter)))

(defn start-scan [activity]
  (let [adapter (get-bluetooth-adapter activity)
        devices (get-bonded-devices adapter)]
    (swap! state/state assoc :devices devices)
    (render-ui activity (scanning-ui activity) (fill-device-list devices)))
  (let [scanner (scanner/start-scan (get-bluetooth-adapter activity) #(add-device activity %))]
    (swap! state/state assoc :scanner scanner)))

(defn stop-scan [activity]
  (scanner/stop-scan (get-bluetooth-adapter activity) (@state/state :scanner))
  (swap! state/state dissoc :scanner)
  (render-ui activity (idle-scan-ui activity) (fill-device-list (@state/state :devices))))

(activity/defactivity
  android.sebluy.gpstracker.ReceivePathActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (when-not (bluetooth-enabled? this)
      (.startActivityForResult this (Intent. BluetoothAdapter/ACTION_REQUEST_ENABLE) 0))
    (start-scan this))
  (onStart
    [this]
    (.superOnStart this)
    (util/keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (util/keep-screen-on this false))
  (onActivityResult
    [this _ result _]
    (if (not= result Activity/RESULT_OK)
      (.finish this))))

