(ns android.sebluy.gpstracker.show-waypoint-path.ui
  (:require [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.ui-utils :as ui-utils]))

(defn ui [state]
  (let [path (get-in state [:page :path])]
    [:linear-layout {:orientation :vertical}
     (ui-utils/table (path/waypoint-attributes path))]))
