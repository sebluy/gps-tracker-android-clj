(comment (ns android.sebluy.gpstracker.remote.activity
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.handlers :as handlers]
            [neko.activity :as activity]
            [android.sebluy.gpstracker.util :as util]))

(activity/defactivity
  android.sebluy.gpstracker.RemoteActivity
  :key :remote
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (state/handle handlers/send-action this))
  (onStart
    [this]
    (.superOnStart this)
    (util/keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (util/keep-screen-on this false))
  (onBackPressed
    [this]
    (state/handle handlers/exit-remote-activity))))


