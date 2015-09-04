(ns android.sebluy.gpstracker.path-list
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [neko.find-view :as find-view])
  (:import [android.widget ArrayAdapter]
           [android R$layout]))

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout {}
       [:list-view {:id ::list-view}]
       [:text-view {:text "List goes here..."}]])))

(activity/defactivity
  android.sebluy.gpstracker.PathListActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)
    (let [[list-view] (find-view/find-views this ::list-view)]
      (.setAdapter list-view (ArrayAdapter. this R$layout/simple_list_item_1 ["hi" "you"])))))

