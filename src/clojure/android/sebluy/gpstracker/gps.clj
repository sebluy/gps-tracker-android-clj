(ns android.sebluy.gpstracker.gps
  (:require [neko.activity :as activity]
            [neko.threading :as threading]))

(defn table-row
  ([title] (table-row title "Pending"))
  ([title value]
   [:table-row
    [:text-view {:text title}]
    [:text-view {:text value}]]))

(defn tracking-table []
  (into [:table-layout]
        (map table-row
             ["Status"
              "Current Speed"
              "Average Speed"
              "Total Distance"
              "Duration"])))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view! activity (tracking-table))))

(activity/defactivity
  android.sebluy.gpstracker.TrackingActivity
  :key :tracking
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)))


