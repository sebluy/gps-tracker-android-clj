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
     :on-click
     (fn [_]
       (state/handle
        remote-handlers/send-request
        {:action :get-paths :path-type :waypoint}))}]
   [:list-view {:id ::list-view}]])

(defn list-click-listener [paths]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      (state/handle common-transitions/navigate {:id :show-waypoint-path
                                                 :path-id ((nth paths position) :id)}))))

(defn fill [{activity :activity paths :waypoint-paths}]
  (let [[^ListView list-view] (find-view/find-views activity ::list-view)
        path-ids (or (mapv :id paths) [])]
    (doto list-view
      (.setAdapter (ArrayAdapter. ^Context activity
                                  ^int R$layout/simple_list_item_1
                                  ^List path-ids))
      (.setOnItemClickListener (list-click-listener paths)))))
