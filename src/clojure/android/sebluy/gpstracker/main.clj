(ns android.sebluy.gpstracker.main
  (:require [neko.activity :as activity]
            [android.sebluy.gpstracker.remote]
            [android.sebluy.gpstracker.gps]
            [neko.intent :as intent]
            [neko.notify :as notify]
            [neko.threading :as threading]))

(defn start-activity [old-activity new-activity]
  (.startActivity old-activity (intent/intent old-activity new-activity {})))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout
       {:orientation :vertical}
       [:text-view {:text "Current Path"}]
       [:button
        {:text "Upload Path"
         :on-click (fn [_] (start-activity activity '.RemoteActivity))}]
       [:button
        {:text "Record Path"
         :on-click (fn [_] (start-activity activity '.TrackingActivity))}]
       [:button
        {:text "Recieve Path"
         :on-click (fn [_] (notify/toast "Clicked Recieve"))}]])))

(activity/defactivity
  android.sebluy.gpstracker.MainActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)))

