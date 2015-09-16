(ns android.sebluy.gpstracker.bluetooth-scanner
  (:require [clojure.core.async :as async])
  (:import [android.bluetooth BluetoothAdapter$LeScanCallback]))

(defn make-scan-callback [chan]
  (reify BluetoothAdapter$LeScanCallback
    (onLeScan [_ device _ _]
      (async/put! chan device))))

(defn stop-scan [adapter callback]
  (.stopLeScan adapter callback))

(defn start-scan [adapter callback]
  (.startLeScan adapter callback))

(defn run [chans bluetooth-adapter]
  (let [scan-callback (make-scan-callback (chans :device))]
    (async/go-loop []
      (while (not= (async/<! (chans :control)) :start))
      (start-scan bluetooth-adapter scan-callback)
      ; wait for timeout or :stop from control chan
      (let [timeout (async/timeout 5000)]
        (while
          (let [[chan-value] (async/alts! [timeout (chans :control)])]
            (not (contains? #{nil :stop} chan-value)))))
      (stop-scan bluetooth-adapter scan-callback)
      (async/put! (chans :status) :stopped)
      (recur))))

