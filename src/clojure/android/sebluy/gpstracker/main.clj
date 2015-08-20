(ns android.sebluy.gpstracker.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.intent :as intent]
            [neko.notify :as notify]
            [neko.threading :refer [on-ui]])
  (:import android.app.Activity))

(defn start-activity [^Activity old-activity new-activity]
  (.startActivity old-activity (intent/intent old-activity new-activity {})))

(defn render-ui [activity]
  (on-ui
    (set-content-view!
      activity
      [:linear-layout
       {:orientation :vertical}
       [:text-view {:text "Current Path"}]
       [:button
        {:text "Upload Path"
         :on-click (fn [_] (start-activity activity '.RemoteActivity))}]
       [:button
        {:text "Record Path"
         :on-click (fn [_] (notify/toast "Clicked Record"))}]
       [:button
        {:text "Recieve Path"
         :on-click (fn [_] (notify/toast "Clicked Recieve"))}]])))

(defactivity
  android.sebluy.gpstracker.MainActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)))


(defactivity
  android.sebluy.gpstracker.RemoteActivity
  :key :remote
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (on-ui
      (set-content-view!
        this
        [:linear-layout
         {:orientation :vertical}
         [:progress-bar {}]
         [:text-view {:text "Status"}]]))))


