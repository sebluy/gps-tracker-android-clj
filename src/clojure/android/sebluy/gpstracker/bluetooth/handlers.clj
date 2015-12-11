(ns android.sebluy.gpstracker.bluetooth.handlers
  (:require [android.sebluy.gpstracker.bluetooth.scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth.loader :as loader]
            [android.sebluy.gpstracker.bluetooth.transitions :as transitions]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.bluetooth BluetoothDevice]
           [android.os Handler]))

(declare attempt-scan)
(declare attempt-write-next)
(declare disconnect)

;; initialization

(defn initialize [{:keys [activity] :as state} request]
  "Sets up essential bluetooth state and then attempts a scan."
  (-> state
      (transitions/initialize
       {:request request
        :status :disconnected
        :adapter (util/bluetooth-adapter activity)
        :devices {}})
      (attempt-scan)))

;; scanning

(defn start-scan [state adapter]
  (let [scanner (scanner/start-scan
                 adapter
                 (fn [device] (state/handle transitions/add-device device)))]
    (transitions/start-scan state scanner)))

(defn attempt-scan [{{:keys [adapter]} :page :as state}]
  (cond-> state
    (util/bluetooth-enabled? adapter)
    (start-scan adapter)))

(defn stop-scan [{activity :activity {:keys [id status scanner adapter]} :page :as state}]
  (scanner/stop-scan adapter scanner)
  (transitions/stop-scan state))

;; connecting

(def loader-callback
  "What to do on loader events"
  (reify loader/LoaderCallback
    (on-connect [_] (state/handle attempt-write-next))
    (on-write [_] (state/handle attempt-write-next))
    (on-disconnect [_] (state/handle disconnect))))

(defn connect [{activity :activity {{path :path} :request} :page :as state} device]
  "Attempts a connection to device. Write will begin immediately after connection
   has been established."
  (let [loader (loader/connect (state :activity) device loader-callback)]
    (transitions/connect state device loader)))

(defn stop-scan-and-connect [state device]
  (-> state
      (stop-scan)
      (connect device)))

;; loading

(defn write [state loader value]
  (if (loader/transmit loader value)
    (transitions/pop-write-queue state)
    (disconnect state)))

(defn attempt-write-next [{{loader :loader write-queue :write-queue} :page :as state}]
  (cond-> state
    (seq write-queue) (write loader (first write-queue))))

;; disconnecting

(defn disconnect [state]
  "Cleans up connection state and records the result of the connection.
   If the write queue is empty on disconnect, then the connection succeeded.
   Otherwise it failed.
   The other end (arduino) should disconnect when it has received all packets
   causing this handler to be triggered."
  (loader/disconnect (get-in state [:page :loader]))
  (transitions/disconnect state))

;; cleaning up

(defn cleanup [{page :page :as state}]
  "Called to clean up bluetooth state (loader and scanner)."
  (when-let [loader (page :loader)]
    (loader/disconnect loader))
  (when-let [scanner (page :scanner)]
    (stop-scan state)))
