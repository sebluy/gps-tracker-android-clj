(ns android.sebluy.gpstracker.common.handlers
  (:require [android.sebluy.gpstracker.common.transitions :as transitions]
            [android.sebluy.gpstracker.bluetooth.handlers :as bluetooth]
            [android.sebluy.gpstracker.remote.handlers :as remote])
  (:import [android.app Activity]))

(defn cleanup [state]
  "Called to cleanup resources allocation by the current page
   before it is replaced by another page."
  (case (get-in state [:page :id])
    :bluetooth (bluetooth/cleanup state)
    :remote (remote/cleanup state)
    nil))

(defn back [state]
  "Handler for managing back button presses."
  (cleanup state)
  (when (empty? (state :history))
    (.finish ^Activity (state :activity)))
  (transitions/back state))
