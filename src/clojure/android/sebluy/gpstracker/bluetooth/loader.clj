(ns android.sebluy.gpstracker.bluetooth.loader
  (:require [android.sebluy.gpstracker.debug :as debug])
  (:import [android.bluetooth BluetoothGattCallback
                              BluetoothProfile
                              BluetoothGattDescriptor]
           [java.util UUID]))

(def CCCD (UUID/fromString "00002902-0000-1000-8000-00805f9b34fb"))
(def RX_SERVICE_UUID (UUID/fromString "6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
(def TX_CHARA_UUID (UUID/fromString "6e400003-b5a3-f393-e0a9-e50e24dcca9e"))
(def RX_CHARA_UUID (UUID/fromString "6e400002-b5a3-f393-e0a9-e50e24dcca9e"))

;; seems to be causing bugs with write
#_(defn enable-tx-notifications [gatt]
  (let [tx (.. gatt (getService RX_SERVICE_UUID) (getCharacteristic TX_CHARA_UUID))
        _ (.setCharacteristicNotification gatt tx true)
        descriptor (.getDescriptor tx CCCD)]
    (.setValue descriptor BluetoothGattDescriptor/ENABLE_NOTIFICATION_VALUE)
    (.writeDescriptor gatt descriptor)))

(defn make-gatt-callback [on-connect on-write on-receive]
  (proxy [BluetoothGattCallback] []
    (onConnectionStateChange
      [gatt status new-state]
      (debug/push {:callback :onConnectionStateChanged
                 :status status
                 :new-state new-state})
      (when (= new-state BluetoothProfile/STATE_CONNECTED)
        (.discoverServices gatt)))
    (onServicesDiscovered [gatt status]
      (debug/push {:callback :onServicesDiscovered
                 :status status})
      (on-connect gatt)
      #_(enable-tx-notifications gatt))
    (onCharacteristicWrite [gatt characteristic status]
      (debug/push {:callback :onCharacteristicWrite
                 :status status})
      (on-write))
    (onCharacteristicChanged
      [gatt characteristic]
      (debug/push {:callback :onCharacteristicChanged})
      (let [value (String. (.getValue characteristic) "UTF-8")]
        (on-receive value)))))

(defn connect [activity device on-connect on-write on-receive]
  (let [gatt (.connectGatt device activity false (make-gatt-callback on-connect on-write on-receive))]
    (.discoverServices gatt)
    gatt))

(defn disconnect [gatt]
  (.close gatt)
  (.disconnect gatt))

(defn transmit [gatt value]
  (let [rx-service (.getService gatt RX_SERVICE_UUID)
        characteristic (.getCharacteristic rx-service RX_CHARA_UUID)]
    (.setValue characteristic value)
    (let [written? (.writeCharacteristic gatt characteristic)]
      (debug/push {:action :transmit :written? written? :value value}))))
