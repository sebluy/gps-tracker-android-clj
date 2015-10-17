(ns android.sebluy.gpstracker.common.handlers
  (:require [android.sebluy.gpstracker.common.transitions :as transitions]))

(defn back [state]
  (when (empty? (state :history))
    (.finish (state :activity)))
  (transitions/back state))
