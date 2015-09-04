(ns android.sebluy.gpstracker.util
  (:require [neko.intent :as intent])
  (:import [android.app Activity]))

(defn start-activity [old-activity new-activity]
  (.startActivity ^Activity old-activity (intent/intent old-activity new-activity {})))
