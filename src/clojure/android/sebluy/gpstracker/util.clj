(ns android.sebluy.gpstracker.util
  (:require [neko.intent :as intent]
            [neko.threading :as threading])
  (:import [android.app Activity]
           [android.view WindowManager$LayoutParams]
           [android.content Context]
           [android.net ConnectivityManager]
           [android.bluetooth BluetoothAdapter]))

(defn start-activity [^Activity old-activity new-activity]
  (.startActivity old-activity (intent/intent old-activity new-activity {})))

(defn keep-screen-on [activity boolean]
  (threading/on-ui
    (let [window (.getWindow activity)]
      (if boolean
        (.addFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)
        (.clearFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)))))



(defn network-available? [^Activity activity]
  (let [connectivity (.getSystemService activity Context/CONNECTIVITY_SERVICE)
        network-info (.getActiveNetworkInfo ^ConnectivityManager connectivity)]
    (and network-info (.isConnected network-info))))

(defn dissoc-in [map path]
  (condp = (count path)
    0 map
    1 (dissoc map (first path))
    (let [sub-path (pop path)
          leaf (get-in map sub-path)
          dissociated (dissoc leaf (peek path))]
      (if (empty? dissociated)
        (dissoc-in map sub-path)
        (assoc-in map sub-path dissociated)))))

