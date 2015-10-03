(ns android.sebluy.gpstracker.bluetooth.activity
  (:require [neko.activity :as activity]
            [android.sebluy.gpstracker.bluetooth.handlers :as handlers]
            [android.sebluy.gpstracker.util :as util]
            [android.sebluy.gpstracker.state :as state]))

(activity/defactivity
  android.sebluy.gpstracker.ReceivePathActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (state/handle handlers/start-scan this))
  (onStart
    [this]
    (.superOnStart this)
    (util/keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (util/keep-screen-on this false)))


