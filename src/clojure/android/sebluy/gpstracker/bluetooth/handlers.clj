(ns android.sebluy.gpstracker.bluetooth.handlers
  (:require [android.sebluy.gpstracker.bluetooth.scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth.loader :as loader]
            [android.sebluy.gpstracker.bluetooth.transitions :as transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]
            [android.sebluy.gpstracker.state :as state]
            [neko.activity :as activity]
            [neko.threading :as threading]
            [neko.find-view :as find-view])
  (:import [android.bluetooth BluetoothDevice]
           [android.os Handler]
           [java.util List]
           [android.content Context]
           [android R$layout]
           [android.widget ArrayAdapter
                           ListView
                           AdapterView$OnItemClickListener]))

(declare update-ui)

;;;;; Handlers ;;;;;

#_(defn transmit-waypoints [serial waypoint-path]
  (loader/transmit serial "start")
  (doseq [value (map str (flatten (map vals waypoint-path)))]
    (loader/transmit serial value))
  (loader/transmit serial "finish"))

(defn add-device [activity device old-state]
  (doto (transitions/add-device old-state device)
    (update-ui activity)))

; stop scan may be handled after scan has already been stopped (sent by scanning timeout),
; thus check status and ignore unless scanning
(defn stop-scan [activity state]
  (if (= (get-in state [:bluetooth :status]) :scanning)
    (do (scanner/stop-scan (util/bluetooth-adapter activity) (get-in state [:bluetooth :scanner]))
        (doto (transitions/stop-scan state)
          (update-ui activity)))
    state))

(defn start-scan [activity state]
  (let [adapter (util/bluetooth-adapter activity)
        devices (util/bonded-devices adapter)
        scanner (scanner/start-scan adapter (fn [device] (state/handle add-device activity device)))]
    (.postDelayed (Handler.) (state/handle stop-scan activity) 5000)
    (doto (transitions/start-scan state devices scanner)
      (update-ui activity))))

(defn receive-path-value [activity value old-state]
  (doto (transitions/receive-path-value old-state value)
    (update-ui activity)))

(defn start-receiving-path [activity device-index old-state]
  (let [devices (get-in old-state [:bluetooth :devices])
        device (-> devices (vals) (nth device-index))
        scanner (get-in old-state [:bluetooth :scanner])
        device-name (.getName ^BluetoothDevice device)]
    (when scanner
      (scanner/stop-scan (util/bluetooth-adapter activity) scanner))
    (loader/new-serial-bluetooth
      activity device identity
      (fn [value] (state/handle receive-path-value activity value)))
    (doto (transitions/start-receiving-path old-state device-name)
      (update-ui activity))))

;;;; UI ;;;;;

(defn scanning-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:text-view {:text "Scanning..."}]
   [:progress-bar {}]
   [:list-view {:id ::list-view}]
   [:button {:text     "Stop"
             :on-click (fn [_] (state/handle stop-scan activity))}]])

(defn idle-scan-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text     "Start"
             :on-click (fn [_] (state/handle start-scan activity))}]])

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

(defn make-list-click-listener [activity]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ device-index _]
      (state/handle start-receiving-path activity device-index))))

(defn fill-device-list [devices]
  (fn [activity]
    (let [[^ListView list-view] (find-view/find-views activity ::list-view)]
      (.setAdapter list-view (ArrayAdapter. ^Context activity ^int R$layout/simple_list_item_1
                                            ^List (or (keys devices) ["No devices"])))
      (.setOnItemClickListener list-view (make-list-click-listener activity)))))

(defn update-ui [state activity]
  (condp = (get-in state [:bluetooth :status])
    :receiving (render-ui activity (loading-ui (get-in state [:bluetooth :device])) identity)
    :idling (render-ui activity (idle-scan-ui activity) (fill-device-list (get-in state [:bluetooth :devices])))
    :success (render-ui activity (summary-ui) identity)
    :scanning (render-ui activity (scanning-ui activity) (fill-device-list (get-in state [:bluetooth :devices])))))

