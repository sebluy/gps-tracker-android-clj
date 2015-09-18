(ns android.sebluy.gpstracker.bluetooth-loader
  (:require [android.sebluy.gpstracker.state :as state])
  (:import [android.bluetooth BluetoothGattCallback
                              BluetoothProfile
                              BluetoothGattDescriptor]
           [java.util UUID]))

(def CCCD (UUID/fromString "00002902-0000-1000-8000-00805f9b34fb"))
(def RX_SERVICE_UUID (UUID/fromString "6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
(def TX_CHARA_UUID (UUID/fromString "6e400003-b5a3-f393-e0a9-e50e24dcca9e"))

(defn push-debug [value]
  (swap! state/state update :debug conj value))

(defn enable-tx-notifications [gatt]
  (push-debug :enabling-tx-notifications)
  (push-debug {:services (map #(.getUuid %) (.getServices gatt))})
  (let [tx (.. gatt (getService RX_SERVICE_UUID) (getCharacteristic TX_CHARA_UUID))
        _ (push-debug tx)
        _ (.setCharacteristicNotification gatt tx true)
        descriptor (.getDescriptor tx CCCD)
        _ (push-debug descriptor)]
    (.setValue descriptor BluetoothGattDescriptor/ENABLE_NOTIFICATION_VALUE)
    (.writeDescriptor gatt descriptor)))

(defn make-gatt-callback [on-value]
  (proxy [BluetoothGattCallback] []
    (onConnectionStateChange
      [gatt status new-state]
      (proxy-super onConnectionStateChange gatt status new-state)
      (push-debug {:new-state new-state})
      (when (= new-state BluetoothProfile/STATE_CONNECTED)
        (push-debug :connected)
        (.discoverServices gatt)))
    (onServicesDiscovered
      [gatt status]
      (proxy-super onServicesDiscovered gatt status)
      (push-debug :services-discovered)
      (enable-tx-notifications gatt))
    (onCharacteristicChanged
      [gatt characteristic]
      (proxy-super onCharacteristicChanged gatt characteristic)
      (let [value (String. (.getValue characteristic) "UTF-8")]
        (push-debug {:value value})
        (on-value value)
        (when (= value "finish")
          (push-debug :done)
          (.close gatt))))))

(defn load-from-device [activity device on-value]
  (.. device (connectGatt activity false (make-gatt-callback on-value)) getServices))


