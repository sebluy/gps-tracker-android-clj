(ns android.sebluy.gpstracker.util
  (:require [neko.intent :as intent]
            [neko.threading :as threading])
  (:import [android.app Activity]
           [android.view WindowManager$LayoutParams]
           [android.content Context]))

(defn start-activity [old-activity new-activity]
  (.startActivity ^Activity old-activity (intent/intent old-activity new-activity {})))

(defn keep-screen-on [activity boolean]
  (threading/on-ui
    (let [window (.getWindow activity)]
      (if boolean
        (.addFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)
        (.clearFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)))))

(defn get-bluetooth-adapter [activity]
  (.. activity (getSystemService Context/BLUETOOTH_SERVICE) getAdapter))

(defn bluetooth-enabled? [activity]
  (let [bluetooth-adapter (get-bluetooth-adapter activity)]
    (and (some? bluetooth-adapter) (.isEnabled bluetooth-adapter))))
