(ns android.sebluy.gpstracker.bluetooth-loader
  (:import [android.bluetooth BluetoothGattCallback
                              BluetoothProfile
                              BluetoothGattDescriptor]
           [java.util UUID]))

(def CCCD (UUID/fromString "00002902-0000-1000-8000-00805f9b34fb"))
(def RX_SERVICE_UUID (UUID/fromString "6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
(def TX_CHARA_UUID (UUID/fromString "6e400003-b5a3-f393-e0a9-e50e24dcca9e"))

(defn enable-tx-notifications [gatt]
  (let [tx (.. gatt (getService RX_SERVICE_UUID) (getCharacteristic TX_CHARA_UUID))
        _ (.setCharacteristicNotification gatt tx true)
        descriptor (.getDescriptor tx CCCD)]
    (.setValue descriptor BluetoothGattDescriptor/ENABLE_NOTIFICATION_VALUE)
    (.writeDescriptor gatt descriptor)))

(defn make-gatt-callback [on-value]
  (proxy [BluetoothGattCallback] []
    (onConnectionStateChange
      [gatt status new-state]
      (proxy-super onConnectionStateChange gatt status new-state)
      (when (= new-state BluetoothProfile/STATE_CONNECTED)
        (.discoverServices gatt)))
    (onServicesDiscovered
      [gatt status]
      (proxy-super onServicesDiscovered gatt status)
      (enable-tx-notifications gatt))
    (onCharacteristicChanged
      [gatt characteristic]
      (proxy-super onCharacteristicChanged gatt characteristic)
      (let [value (String. (.getValue characteristic) "UTF-8")]
        (when (= value "finish")
          (.close gatt))
        (on-value value)))))

(defn load-from-device [activity device on-value]
  (.. device (connectGatt activity false (make-gatt-callback on-value)) getServices))


