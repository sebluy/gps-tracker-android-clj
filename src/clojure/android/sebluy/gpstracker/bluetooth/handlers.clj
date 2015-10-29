(ns android.sebluy.gpstracker.bluetooth.handlers
  (:require [android.sebluy.gpstracker.bluetooth.scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth.loader :as loader]
            [android.sebluy.gpstracker.bluetooth.transitions :as transitions]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.bluetooth BluetoothDevice]
           [android.os Handler]))

(defn start-loading [{{loader :loader {path :path} :request} :page :as state}]
  ;; maybe make this async because it might block thread
  (android.sebluy.gpstracker.debug/push :loading)
  (loader/transmit loader "start")
  (doseq [value (map str (flatten (map vals (path :points))))]
    (Thread/sleep 100)
    (loader/transmit loader value))
  (loader/transmit loader "finish")
  (loader/disconnect loader)
  (update state :page (fn [page] (-> page
                                     (dissoc :loader)
                                     (assoc :status :finished)))))

(-> @state/state :page :request :path)

(defn connect [{:keys [activity] :as state} device]
  ;; no on receive yet
  (android.sebluy.gpstracker.debug/push :connecting)
  (let [loader (loader/connect activity device (fn [gatt] (state/handle start-loading)) identity)]
    (update state :page assoc :loader loader :status :pending :device device)))

; stop scan may be handled after scan has already been stopped (sent by scanning timeout),
; thus check status and ignore unless scanning
(defn stop-scan [{activity :activity {:keys [id status scanner adapter]} :page :as state}]
  ; maybe add some sort of preconditions helper if this kind of checking becomes ubiquitous and tedious
  (if (and (= id :bluetooth) (= status :scanning))
    (do (scanner/stop-scan adapter scanner)
        (transitions/stop-scan state))
    state))

(defn stop-scan-and-connect [state device]
  (-> state
      (stop-scan)
      (connect device)))

;; helper, not handler maybe move to "side effects" namespace
(defn start-scan [adapter]
  (let [scanner (scanner/start-scan adapter (fn [device] (state/handle transitions/add-device device)))]
    ;; maybe use ui thread instead of creating a new thread
    (future (Thread/sleep 5000) (state/handle stop-scan))
    scanner))

(defn attempt-scan [{{:keys [adapter]} :page :as state}]
  (if (util/bluetooth-enabled? adapter)
    (let [devices (util/bonded-devices adapter)
          scanner (start-scan adapter)]
      (transitions/start-scan state {:scanner scanner :devices devices}))
      state))

;; checks to see if bluetooth is connected
;; if it is start scanning and set page to scanning
;; else set state to disconnected
(defn initialize [{:keys [activity] :as state} request]
  (let [adapter (util/bluetooth-adapter activity)]
    (if (util/bluetooth-enabled? adapter)
      (let [scanner (start-scan adapter)
            devices (util/bonded-devices adapter)]
        (transitions/initialize state {:request request
                                       :status :scanning
                                       :adapter adapter
                                       :scanner scanner
                                       :devices devices}))
      (transitions/initialize state {:request request
                                     :status :disconnected
                                     :adapter adapter
                                     :devices {}}))))


#_(defn start-receiving-path [state device-index]
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
