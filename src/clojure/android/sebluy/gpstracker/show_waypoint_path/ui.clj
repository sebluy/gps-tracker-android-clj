(ns android.sebluy.gpstracker.show-waypoint-path.ui
  (:require [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.ui :as ui]))

(defn ui [state]
  (let [path (get-in state [:page :path])]
    [:linear-layout {:orientation :vertical}
     (ui/table (path/waypoint-attributes path))]))

(-> @android.sebluy.gpstracker.state/state :page :path path/total-distance)
