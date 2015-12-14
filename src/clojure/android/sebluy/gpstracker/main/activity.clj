(ns android.sebluy.gpstracker.main.activity
  (:require [neko.activity :as activity]
            [android.sebluy.gpstracker.ui]
            [android.sebluy.gpstracker.util :as util]
            [android.sebluy.gpstracker.common.transitions :as transitions]
            [android.sebluy.gpstracker.common.handlers :as handlers]
            [android.sebluy.gpstracker.state :as state]))

(activity/defactivity
  android.sebluy.gpstracker.MainActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (util/keep-screen-on this true)
    (state/handle transitions/initialize this))
  (onBackPressed
    [this]
    (state/handle handlers/back)))
