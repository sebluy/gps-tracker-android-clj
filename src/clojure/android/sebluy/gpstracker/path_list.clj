(ns android.sebluy.gpstracker.path-list
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [neko.find-view :as find-view]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.util :as util])
  (:import [android.widget ArrayAdapter
                           AdapterView$OnItemClickListener]
           [android R$layout]))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout {}
       [:list-view {:id ::list-view}]
       [:text-view {:text "List goes here..."}]])))

(defn make-list-click-listener [activity]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      (swap! state/state assoc :show-path (-> @state/state :paths vals (nth position)))
      (util/start-activity activity '.ShowPathActivity))))

(activity/defactivity
  android.sebluy.gpstracker.PathListActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)
    (let [[list-view] (find-view/find-views this ::list-view)]
      (.setAdapter list-view (ArrayAdapter. this R$layout/simple_list_item_1
                                            (or (keys (@state/state :paths)) [])))
      (.setOnItemClickListener list-view (make-list-click-listener this)))))

