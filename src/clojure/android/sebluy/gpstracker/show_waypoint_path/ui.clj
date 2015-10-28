(ns android.sebluy.gpstracker.show-waypoint-path.ui
  (:require [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.bluetooth.handlers :as bluetooth-handlers]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.ui-utils :as ui-utils]))

(defn ui [state]
  (let [path-id (get-in state [:page :path-id])
        ;; this code is used in browser client as well, refactor to common
        path (->> (get-in state [:waypoint-paths])
                  (filter (fn [path] (= (path :id) path-id)))
                  first)]
    [:linear-layout {:orientation :vertical}
     (ui-utils/table (path/waypoint-attributes path))
     [:button {:text "Send to Arduino"
               :on-click
               (fn [_]
                 (state/handle bluetooth-handlers/send-waypoint path))}]]))
