(comment
  (ns android.sebluy.gpstracker.main
    (:require [neko.activity :as activity]
      ; require activity namespaces
              [android.sebluy.gpstracker.remote.activity]
              [android.sebluy.gpstracker.gps-old]
              [android.sebluy.gpstracker.path-list]
              [android.sebluy.gpstracker.show-path]
              [android.sebluy.gpstracker.util :as util]
              [android.sebluy.gpstracker.bluetooth.util :as bluetooth-util]
              [neko.threading :as threading]
              [neko.debug :as neko-debug]
              [android.sebluy.gpstracker.state :as state])
    (:import [android.content Intent]
             [android.bluetooth BluetoothAdapter]
             [android.app Activity]))

  (defn render-ui [^Activity activity]
    (threading/on-ui
      (activity/set-content-view!
        activity
        [:linear-layout
         {:orientation :vertical}
         [:button
          {:text     "View Tracking Paths"
           :on-click (fn [_] (util/start-activity activity '.PathListActivity))}]
         [:button
          {:text     "View Waypoint Paths"
           :on-click (fn [_] (util/start-activity activity '.WaypointPathListActivity))}]
         [:button
          {:text     "Record Path"
           :on-click (fn [_] (util/start-activity activity '.TrackingActivity))}]
         [:button
          {:text     "Recieve Path"
           :on-click (fn [_]
                       (if (bluetooth-util/bluetooth-enabled? activity)
                         (util/start-activity activity '.ReceiveActivity)
                         (.startActivityForResult activity (Intent. BluetoothAdapter/ACTION_REQUEST_ENABLE) 0)))}]])))

  (defn set-page [page state]
    (assoc state :page page))

  (defn stash-activity [state activity]
    (assoc state :activity activity))

  (defn initialize [state activity]
    (-> state
        (set-page-main)
        (stash-activity activity)))

  ;; add watch to state that updates ui on change
  (activity/defactivity
    android.sebluy.gpstracker.MainActivity
    :key :main
    (onCreate
      [this bundle]
      (.superOnCreate this bundle)
      (neko-debug/keep-screen-on this)
      (state/handle initialize this))))
