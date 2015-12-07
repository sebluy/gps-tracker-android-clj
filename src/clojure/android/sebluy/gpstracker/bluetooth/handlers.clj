(ns android.sebluy.gpstracker.bluetooth.handlers
  (:require [android.sebluy.gpstracker.bluetooth.scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth.loader :as loader]
            [android.sebluy.gpstracker.bluetooth.transitions :as transitions]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.bluetooth BluetoothDevice]
           [android.os Handler]))

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

(def loader-callback
  (reify loader/LoaderCallback
    (on-connect [_] (state/handle start-loading))
    (on-write [_] (state/handle start-loading))
    (on-disconnect [_] (state/handle disconnect))))

(defn connect [{activity :activity {{path :path} :request} :page :as state} device]
  (let [loader (loader/connect activity device loader-callback)]
    (update state :page assoc
            :loader loader
            :status :pending
            :device device
            :write-queue (serialize-path path))))

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

(defn cleanup [{page :page :as state}]
  "Called to clean up bluetooth state."
  (when-let [loader (page :loader)]
    (loader/disconnect loader))
  (when-let [scanner (page :scanner)]
    (stop-scan state)))
