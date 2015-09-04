(ns android.sebluy.gpstracker.show-path
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [android.sebluy.gpstracker.state :as state]))

(defn render-ui [activity path]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout {}
       [:text-view {:text (path :created-at)}]])))

(activity/defactivity
  android.sebluy.gpstracker.ShowPathActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this (@state/state :show-path))))

