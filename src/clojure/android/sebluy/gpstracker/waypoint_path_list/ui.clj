(ns android.sebluy.gpstracker.waypoint-path-list.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.handlers :as remote-handlers]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [neko.find-view :as find-view])
  (:import [android.widget AdapterView$OnItemClickListener ArrayAdapter ListView]
           [android.content Context]
           [java.util List]
           [android R$layout]))

(def ui
  [:linear-layout {:orientation :vertical}
   [:button
    {:text     "Refresh"
     :on-click (fn [_] (state/handle remote-handlers/send-request :get-waypoint-paths))}]
   [:list-view {:id ::list-view}]])

(defn list-click-listener [paths]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      (state/handle common-transitions/navigate {:id :show-waypoint-path
                                                 :path (nth paths position)}))))

(defn fill [state activity]
  (let [[^ListView list-view] (find-view/find-views activity ::list-view)
        path-map (state :waypoint-paths)
        path-ids (or (keys path-map) [])
        path-values (vals path-map)]
    (doto list-view
      (.setAdapter (ArrayAdapter. ^Context activity
                                  ^int R$layout/simple_list_item_1
                                  ^List path-ids))
      (.setOnItemClickListener (list-click-listener path-values)))))
