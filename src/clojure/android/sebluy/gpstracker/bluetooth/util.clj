(ns android.sebluy.gpstracker.bluetooth.util
  (:import [android.bluetooth BluetoothDevice
                              BluetoothAdapter]
           [android.content Context]
           [android.app Activity]))

(defn device-key [^BluetoothDevice device]
  (or (.getName device) (.getAddress device)))

(defn bonded-devices [^BluetoothAdapter adapter]
  (into {} (map
            (fn [device] [(device-key device) device])
            (.getBondedDevices adapter))))

(defn bluetooth-adapter [^Activity activity]
  (.. activity (getSystemService Context/BLUETOOTH_SERVICE) getAdapter))

(defn bluetooth-enabled? [^BluetoothAdapter adapter]
  (and (some? adapter) (.isEnabled adapter)))
