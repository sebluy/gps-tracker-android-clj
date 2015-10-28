(ns android.sebluy.gpstracker.bluetooth.handlers
  (:require [android.sebluy.gpstracker.bluetooth.scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth.loader :as loader]
            [android.sebluy.gpstracker.bluetooth.transitions :as transitions]
            [android.sebluy.gpstracker.bluetooth.common-transitions :as common-transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.bluetooth BluetoothDevice]
           [android.os Handler]))

#_(defn transmit-waypoints [serial waypoint-path]
  (loader/transmit serial "start")
  (doseq [value (map str (flatten (map vals waypoint-path)))]
    (loader/transmit serial value))
  (loader/transmit serial "finish"))

(defn send-waypoint [state path]
  (common-transitions/navigate state {:id :bluetooth}))

; stop scan may be handled after scan has already been stopped (sent by scanning timeout),
; thus check status and ignore unless scanning
(defn stop-scan [state]
  ; maybe add some sort of preconditions helper if this kind of checking becomes ubiquitous and tedious
  (if (= (get-in state [:bluetooth :status]) :scanning)
    (do (scanner/stop-scan (util/bluetooth-adapter (state :activity)) (get-in state [:bluetooth :scanner]))
        (transitions/stop-scan state))
    state))

;; maybe maintain bluetooth-adapter in state
(defn start-scan [state]
  (let [activity (state :activity)
        adapter (util/bluetooth-adapter activity)
        devices (util/bonded-devices adapter)
        scanner (scanner/start-scan adapter (fn [device] (state/handle add-device device)))]
    ; maybe just thread/sleep instead of using ui thread
    (.postDelayed (Handler.) (state/handle stop-scan) 5000)
    (transitions/start-scan state devices scanner)))

(defn start-receiving-path [state device-index]
  (let [activity (state :activity)
        devices (get-in state [:bluetooth :devices])
        device (-> devices (vals) (nth device-index))
        scanner (get-in state [:bluetooth :scanner])
        device-name (.getName ^BluetoothDevice device)]
    (when scanner
      (scanner/stop-scan (util/bluetooth-adapter activity) scanner))
    (loader/new-serial-bluetooth
      activity device identity
      (fn [value] (state/handle transitions/receive-path-value value)))
    (transitions/start-receiving-path state device-name)))
