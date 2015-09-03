(ns android.sebluy.gpstracker.path-list
  (:require [neko.activity :as activity]
            [neko.threading :as threading]))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout {}
       [:text-view {:text "List goes here..."}]])))

(activity/defactivity
  android.sebluy.gpstracker.PathListActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)))

