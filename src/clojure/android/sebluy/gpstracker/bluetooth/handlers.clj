(ns android.sebluy.gpstracker.bluetooth.handlers
  (:require [android.sebluy.gpstracker.bluetooth.scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth.loader :as loader]
            [android.sebluy.gpstracker.bluetooth.transitions :as transitions]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.bluetooth BluetoothDevice]
           [android.os Handler]))

(abstract-map/extend-schema
 BluetoothPage Page [:bluetooth] {:status s/Keyword
                                  :request s/Any
                                  :adapter s/Any
                                  :devices s/Any
                                  (s/optional-key :write-queue) s/Any
                                  (s/optional-key :device) s/Any
                                  (s/optional-key :loader) s/Any
                                  (s/optional-key :scanner) s/Any})


(defn start-loading [{{loader :loader write-queue :write-queue} :page :as state}]
  "If there is data in write queue, send it and dequeue it."
  (if (seq write-queue)
    (do (loader/transmit loader (first write-queue))
        (update-in state [:page :write-queue] (fn [queue] (drop 1 queue))))
    state))

(defn serialize-path [{:keys [points]}]
  "Create a list of strings starting with waypoint count followed by each
   waypoint field in format '(count lat-1 lng-1 lat-2 lng-2 ...).
   e.g. '(3 1.034 2.199 1.035 2.200 3.994 5.947) with each value as a string."
  (cons (-> points count str) (->> points (map vals) flatten (map str))))

(defn disconnect [{{write-queue :write-queue} :page :as state}]
  "Cleans up connection state and records the result of the connection.
   If the write queue is empty on disconnect, then the connection succeeded.
   Otherwise it failed.
   The other end (arduino) should disconnect when it has received all packets."
  (let [result (if (empty? write-queue) :success :failure)]
    (update state :page (fn [page] (-> page
                                       (dissoc :loader)
                                       (dissoc :write-queue)
                                       (assoc :status result))))))

(defn connect [{activity :activity {{path :path} :request} :page :as state} device]
  ;; no on receive yet
  (let [loader (loader/connect activity device
                               (fn [gatt] (state/handle start-loading))
                               (fn [] (state/handle start-loading))
                               identity
                               (fn [] (state/handle disconnect)))]
    (update state :page assoc
            :loader loader
            :status :pending
            :device device
            :write-queue (serialize-path path))))

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

(defn cleanup [{page :page :as state}]
  "Called to clean up bluetooth state."
  (when-let [loader (page :loader)]
    (loader/disconnect loader))
  (when-let [scanner (page :scanner)]
    (stop-scan state)))
