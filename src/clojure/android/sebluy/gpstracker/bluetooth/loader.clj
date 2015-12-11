(ns android.sebluy.gpstracker.bluetooth.loader
  (:import [android.bluetooth BluetoothGattCallback
                              BluetoothProfile
                              BluetoothGattDescriptor]
           [java.util UUID]))

(def RX_SERVICE_UUID (UUID/fromString "6e400001-b5a3-f393-e0a9-e50e24dcca9e"))
(def RX_CHARA_UUID (UUID/fromString "6e400002-b5a3-f393-e0a9-e50e24dcca9e"))

(defprotocol LoaderCallback
  "Wrapper protocol around BluetoothGattCallback to simplify interface."
  (on-connect [this])
  (on-write [this])
  (on-disconnect [this]))

(defn make-gatt-callback [loader-callback]
  (proxy [BluetoothGattCallback] []
    (onConnectionStateChange
      [gatt _ new-state]
      (when (= new-state BluetoothProfile/STATE_CONNECTED)
        (.discoverServices gatt))
      (when (= new-state BluetoothProfile/STATE_DISCONNECTED)
        (on-disconnect loader-callback)))
    (onServicesDiscovered [_ _]
      (on-connect loader-callback))
    (onCharacteristicWrite [_ _ _]
      (on-write loader-callback))))

(defn connect [activity device loader]
  (doto (.connectGatt device activity false (make-gatt-callback loader))
    (.discoverServices)))

;; causing problems with invalid bluetooth device
(defn disconnect [gatt]
  (.close gatt)
  (.disconnect gatt))

(defn uart-characteristic [gatt]
  (some-> gatt
          (.getService RX_SERVICE_UUID)
          (.getCharacteristic RX_CHARA_UUID)))

(defn transmit [gatt value]
  "Attempts to transmit value over gatt connection.
   Returns true iff transmit initiated successfully."
  (if-let [characteristic (uart-characteristic gatt)]
    (do
      (.setValue characteristic value)
      (.writeCharacteristic gatt characteristic))
    false))
