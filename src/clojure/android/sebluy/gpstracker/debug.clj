(ns android.sebluy.gpstracker.debug
  (:require [android.sebluy.gpstracker.state :as state]))

(defn push [value]
  (swap! state/state update :debug conj value))

