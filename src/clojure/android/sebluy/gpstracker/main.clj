(ns android.sebluy.gpstracker.main
  (:require [neko.activity :as activity]
    ; require activity namespaces
            [android.sebluy.gpstracker.remote.activity]
            [android.sebluy.gpstracker.gps]
            [android.sebluy.gpstracker.path-list]
            [android.sebluy.gpstracker.show-path]
            [android.sebluy.gpstracker.util :as util]
            [neko.threading :as threading])
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
        {:text      "View Waypoint Paths"
         :on-click (fn [_] (util/start-activity activity '.WaypointPathListActivity))}]
       [:button
        {:text     "Record Path"
         :on-click (fn [_] (util/start-activity activity '.TrackingActivity))}]
       [:button
        {:text     "Recieve Path"
         :on-click (fn [_]
                     (if (util/bluetooth-enabled? activity)
                       (util/start-activity activity '.ReceivePathActivity)
                       (.startActivityForResult activity (Intent. BluetoothAdapter/ACTION_REQUEST_ENABLE) 0)))}]])))

(activity/defactivity
  android.sebluy.gpstracker.MainActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this))
  (onActivityResult
    [this _ result _]
    (when (= result Activity/RESULT_OK)
      (util/start-activity this '.ReceivePathActivity))))

