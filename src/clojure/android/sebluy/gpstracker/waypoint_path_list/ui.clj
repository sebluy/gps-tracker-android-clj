(ns android.sebluy.gpstracker.waypoint-path-list.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.handlers :as remote-handlers]
            [neko.find-view :as find-view])
  (:import (android.widget AdapterView$OnItemClickListener ArrayAdapter ListView)
           (android.content Context)
           (java.util List)
           (android R$layout)))

(def ui
  [:linear-layout {:orientation :vertical}
   [:button
    {:text     "Refresh"
     :on-click (fn [_] (state/handle remote-handlers/send-request :get-waypoint-paths))}]
   [:list-view {:id ::list-view}]])

(def list-click-listener
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      #_(swap! state/state assoc :show-waypoint-path (-> @state/state :waypoint-paths vals (nth position)))
      #_(util/start-activity activity '.ShowWaypointPathActivity))))

(defn fill [state activity]
  (let [[^ListView list-view] (find-view/find-views activity ::list-view)
        waypoint-list (or (keys (state :waypoint-paths)) [])]
    (doto list-view
      (.setAdapter (ArrayAdapter. ^Context activity
                                  ^int R$layout/simple_list_item_1
                                  ^List waypoint-list))
      (.setOnItemClickListener list-click-listener))))


