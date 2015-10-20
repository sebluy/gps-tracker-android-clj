(ns android.sebluy.gpstracker.main.ui
 (:require [android.sebluy.gpstracker.state :as state]
           [android.sebluy.gpstracker.common.transitions :as transitions]))

(defn ui [_]
  [:linear-layout
   {:orientation :vertical}
   [:button
    {:text "View Tracking Paths"}]
   [:button
    {:text     "View Waypoint Paths"
     :on-click (fn [_] (state/handle transitions/navigate {:id :waypoint-path-list}))}]
   [:button
    {:text "Record Path"}]
   [:button
    {:text "Recieve Path"}]])
