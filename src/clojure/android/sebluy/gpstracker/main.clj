(ns android.sebluy.gpstracker.main
  (:require [neko.activity :as activity]
            ; require activity namespaces
            [android.sebluy.gpstracker.remote]
            [android.sebluy.gpstracker.gps]
            [android.sebluy.gpstracker.path-list]
            [android.sebluy.gpstracker.show-path]
            [android.sebluy.gpstracker.util :as util]
            [neko.threading :as threading]))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout
       {:orientation :vertical}
       [:text-view {:text "Current Path"}]
       [:button
        {:text "View Paths"
         :on-click (fn [_] (util/start-activity activity '.PathListActivity))}]
       [:button
        {:text "Record Path"
         :on-click (fn [_] (util/start-activity activity '.TrackingActivity))}]
       [:button
        {:text "Recieve Path"
         :on-click (fn [_] (util/start-activity activity '.ReceivePathActivity))}]])))

(activity/defactivity
  android.sebluy.gpstracker.MainActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)))

