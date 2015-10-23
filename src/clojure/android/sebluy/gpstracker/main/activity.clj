(ns android.sebluy.gpstracker.main.activity
  (:require [neko.activity :as activity]
            [neko.debug :as neko-debug]
            [android.sebluy.gpstracker.ui]
            [android.sebluy.gpstracker.common.handlers :as handlers]
            [android.sebluy.gpstracker.main.transitions :as transitions]
            [android.sebluy.gpstracker.state :as state]))

; reset state for debugging
;(restart-agent state/state {})
;(state/handle transitions/initialize (neko.debug/*a :main))
;(state/handle (fn [state] (assoc state :page {:id :main})))
;(identity @state/state)

;; add watch to state that updates ui on change
(activity/defactivity
  android.sebluy.gpstracker.MainActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (state/initialize this))
  (onBackPressed
    [this]
    (state/handle handlers/back)))
