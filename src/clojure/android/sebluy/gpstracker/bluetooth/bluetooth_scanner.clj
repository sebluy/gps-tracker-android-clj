(ns android.sebluy.gpstracker.bluetooth-scanner
  (:import [android.bluetooth BluetoothAdapter$LeScanCallback]))

(defn make-scan-callback [on-device-found]
  (reify BluetoothAdapter$LeScanCallback
    (onLeScan [_ device _ _]
      (on-device-found device))))

(defn stop-scan [adapter callback]
  (.stopLeScan adapter callback))

(defn start-scan [adapter on-device-found]
  (let [callback (make-scan-callback on-device-found)]
    (.startLeScan adapter callback)
    callback))

