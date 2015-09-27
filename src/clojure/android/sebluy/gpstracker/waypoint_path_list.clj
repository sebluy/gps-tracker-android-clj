(ns android.sebluy.gpstracker.waypoint-path-list
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [neko.find-view :as find-view]
            [neko.ui.menu :as menu]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.util :as util])
  (:import [android.widget ArrayAdapter
                           AdapterView$OnItemClickListener ListView]
           [android R$layout]
           [java.util List]
           [android.content Context]))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout {}
       [:button {:text "Refresh"
                 :on-click (fn [_] )}]
       [:list-view {:id ::list-view}]])))

(defn make-list-click-listener [activity]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      (swap! state/state assoc :show-waypoint-path (-> @state/state :waypoint-paths vals (nth position)))
      (util/start-activity activity '.ShowWaypointPathActivity))))

(activity/defactivity
  android.sebluy.gpstracker.WaypointPathListActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)
    (let [[^ListView list-view] (find-view/find-views this ::list-view)]
      (.setAdapter list-view (ArrayAdapter. ^Context this ^int R$layout/simple_list_item_1
                                            ^List (or (keys (@state/state :waypoint-paths)) [])))
      (.setOnItemClickListener list-view (make-list-click-listener this)))))

