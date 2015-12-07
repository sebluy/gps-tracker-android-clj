(ns android.sebluy.gpstracker.waypoint-path-list.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.handlers :as remote-handlers]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [android.sebluy.gpstracker.util :as util]
            [neko.find-view :as find-view])
  (:import [android.widget AdapterView$OnItemClickListener ArrayAdapter ListView]
           [android.content Context]
           [java.util List]
           [android R$layout]))

(defn ui [{waypoint-paths :waypoint-paths}]
  [:linear-layout {:orientation :vertical}
   [:button
    {:text "Refresh"
     :layout-margin 10
     :padding 30
     :layout-width :fill-parent
     :on-click
     (fn [_]
       (state/handle
        remote-handlers/send-request
        {:action :get-paths :path-type :waypoint}))}]
   (if (seq waypoint-paths)
     [:list-view {:id ::list-view}]
     [:text-view {:text "No waypoints."
                  :layout-width :fill-parent
                  :layout-height :fill-parent
                  :gravity :center}])])

(defn list-click-listener [paths]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      (state/handle common-transitions/navigate {:id :show-waypoint-path
                                                 :path-id ((nth paths position) :id)}))))

(defn paths->showable-list [paths]
  "Given a vector of waypoint paths, returns a vector of strings
   identifying each path."
  (mapv (fn [path] (-> path :id util/date->string))
            paths))

(defn fill [{activity :activity paths :waypoint-paths}]
  "Fills in the list view with each waypoint path."
  (if (seq paths)
    (let [[^ListView list-view] (find-view/find-views activity ::list-view)
          path-ids (paths->showable-list paths)]
      (doto list-view
        (.setAdapter (ArrayAdapter. ^Context activity
                                    ^int R$layout/simple_list_item_1
                                    ^List path-ids))
        (.setOnItemClickListener (list-click-listener paths))))))
