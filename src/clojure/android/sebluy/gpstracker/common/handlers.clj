(ns android.sebluy.gpstracker.common.handlers
  (:require [android.sebluy.gpstracker.common.transitions :as transitions]
            [android.sebluy.gpstracker.bluetooth.handlers :as bluetooth]))

(defn cleanup [state]
  (case (get-in state [:page :id])
    :bluetooth (bluetooth/cleanup state)
    nil))

(defn back [state]
  (cleanup state)
  (when (empty? (state :history))
    (.finish (state :activity)))
  (transitions/back state))
