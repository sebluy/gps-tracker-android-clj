(ns android.sebluy.gpstracker.show-waypoint-path
  (:require [neko.activity :as activity]
            [neko.threading :as threading]))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout {:orientation :vertical}
       [:text-view {:text "Empty for now..."}]])))

(activity/defactivity
  android.sebluy.gpstracker.ShowWaypointPathActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this))
  (onBackPressed
    [this]
    (.superOnBackPressed this)))

