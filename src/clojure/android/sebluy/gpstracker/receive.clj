(ns android.sebluy.gpstracker.receive
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [android.sebluy.gpstracker.util :as util]
            [android.sebluy.gpstracker.bluetooth-scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth-loader :as loader]
            [neko.find-view :as find-view]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.debug :as debug])
  (:import [android.content Context]
           [android.bluetooth BluetoothAdapter BluetoothDevice]
           [android R$layout]
           [android.widget ArrayAdapter
                           AdapterView$OnItemClickListener ListView]
           [android.os Handler]
           [java.util List]))

(comment
(def device (get-in @state/state [:devices "UART"]))
(def activity (neko.debug/*a))
(@state/state :debug)
(debug/push "working")
(def serial (loader/new-serial-bluetooth activity device
                                         #(loader/transmit % "transmit-on-connect")
                                         #(debug/push (str "Received " %)))))

;(dotimes [n 10]
;  (loader/transmit serial (str n)))
;(loader/disconnect serial)
;(@state/state :debug)
;device

#_(def waypoint-path [{:latitude 1.0 :longitude 2.0}
                    {:latitude 3.0 :longitude 4.0}])

(defn transmit-waypoints [serial waypoint-path]
  (loader/transmit serial "start")
  (doseq [value (map str (flatten (map vals waypoint-path)))]
    (loader/transmit serial value))
  (loader/transmit serial "finish"))


;(transmit-waypoints serial waypoint-path)

(declare stop-scan start-scan)

(defn scanning-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:text-view {:text "Scanning..."}]
   [:progress-bar {}]
   [:list-view {:id ::list-view}]
   [:button {:text     "Stop"
             :on-click (fn [_] (stop-scan activity))}]])

(defn idle-scan-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text     "Start"
             :on-click (fn [_] (start-scan activity))}]])

(defn loading-ui [device-name]
  [:linear-layout {:orientation :vertical}
   [:text-view {:text (str "Loading..." device-name)}]
   [:progress-bar {}]])

(defn summary-ui []
  [:linear-layout {}
   [:text-view {:text "Success"}]])

(defn render-ui [activity ui post-render-fn]
  (threading/on-ui
    (activity/set-content-view! activity ui)
    (post-render-fn activity)))

(defn add-path [state path]
  (assoc-in state [:paths (path :created-at)] path))

(defn load-path-from-device [activity device]
  (swap! state/state assoc :values [])
  (loader/new-serial-bluetooth
    activity device
    identity
    (fn [value]
      (swap! state/state update :values conj value)
      (when (= value "finish")
        (swap! state/state (fn [state]
                             (-> state
                                 (add-path (path/make-new (path/parse-path (state :values))))
                                 (dissoc :devices :state :values))))
        (render-ui activity (summary-ui) identity))))
  (render-ui activity (loading-ui (.getName ^BluetoothDevice device)) identity))

(defn make-list-click-listener [activity]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      (swap! state/state assoc :state :loading)
      (stop-scan activity)
      (load-path-from-device activity (nth (vals (@state/state :devices)) position)))))

(defn fill-device-list [devices]
  (fn [activity]
    (let [[^ListView list-view] (find-view/find-views activity ::list-view)]
      (.setAdapter list-view (ArrayAdapter. ^Context activity ^int R$layout/simple_list_item_1
                                            ^List (or (keys devices) ["No devices"])))
      (.setOnItemClickListener list-view (make-list-click-listener activity)))))

(defn device-key [^BluetoothDevice device]
  (or (.getName device) (.getAddress device)))

(defn add-device [activity device]
  (let [key (device-key device)]
    (swap! state/state assoc-in [:devices key] device)
    (render-ui activity (scanning-ui activity) (fill-device-list (@state/state :devices)))))

(defn get-bonded-devices [^BluetoothAdapter adapter]
  (reduce (fn [devices device]
            (assoc devices (device-key device) device))
          {} (.getBondedDevices adapter)))

(defn start-scan [activity]
  (let [adapter (util/get-bluetooth-adapter activity)
        devices (get-bonded-devices adapter)]
    (swap! state/state assoc :devices devices)
    (render-ui activity (scanning-ui activity) (fill-device-list devices)))
  (let [scanner (scanner/start-scan (util/get-bluetooth-adapter activity) #(add-device activity %))]
    (swap! state/state assoc :scanner scanner :state :scanning))
  (.postDelayed (Handler.) #(stop-scan activity) 5000))

(defn stop-scan [activity]
  (when-let [scanner (@state/state :scanner)]
    (scanner/stop-scan (util/get-bluetooth-adapter activity) scanner)
    (swap! state/state dissoc :scanner))
  (when (= (@state/state :state) :scanning)
    (swap! state/state assoc :state :idling)
    (render-ui activity (idle-scan-ui activity) (fill-device-list (@state/state :devices)))))

(activity/defactivity
  android.sebluy.gpstracker.ReceivePathActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (start-scan this))
  (onStart
    [this]
    (.superOnStart this)
    (util/keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (util/keep-screen-on this false)))


