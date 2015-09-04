(ns android.sebluy.gpstracker.util
  (:require [neko.intent :as intent]
            [neko.threading :as threading])
  (:import [android.app Activity]
           [android.view WindowManager$LayoutParams]))

(defn start-activity [old-activity new-activity]
  (.startActivity ^Activity old-activity (intent/intent old-activity new-activity {})))

(defn keep-screen-on [activity boolean]
  (threading/on-ui
    (let [window (.getWindow activity)]
      (if boolean
        (.addFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)
        (.clearFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)))))

