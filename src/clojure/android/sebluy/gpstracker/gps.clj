(ns android.sebluy.gpstracker.gps
  (:require [neko.activity :as activity]
            [neko.ui.mapping :as mapping]
            [neko.threading :as threading])
  (:import (android.widget TableLayout
                           TableRow)
           (android.view WindowManager$LayoutParams)))

#_(defn make-google-api-client [activity]
  (GoogleApiClient$Builder. activity))

#_(defn run-gps [activity]
  (let [control (async/chan)
        location (async/chan)
        status (async/chan)]))

(mapping/defelement
  :table-layout
  :classname TableLayout
  :inherits :view-group)

(mapping/defelement
  :table-row
  :classname TableRow
  :inherits :view)

(defn table-row
  ([title] (table-row title "Pending"))
  ([title value]
   [:table-row {}
    [:text-view {:text title}]
    [:text-view {:text value}]]))

(defn tracking-table []
  (into [:table-layout {}]
        (map table-row
             ["Status"
              "Current Speed"
              "Average Speed"
              "Total Distance"
              "Duration"])))

(defn ui [activity]
  [:linear-layout {}
   (tracking-table)
   [:button {:text "Pause"}]
   [:button {:text "Finish"
             :on-click (fn [_] (.finish activity))}]])

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view! activity (ui activity))))

(defn keep-screen-on [activity]
  (threading/on-ui
    (.. activity
        getWindow
        (addFlags WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON))))

(defn disable-keep-screen-on [activity]
    (threading/on-ui
    (.. activity
        getWindow
        (clearFlags WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON))))

(activity/defactivity
  android.sebluy.gpstracker.TrackingActivity
  :key :tracking
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this))
  (onStart
    [this]
    (.superOnStart this)
    (keep-screen-on this))
  (onStop
    [this]
    (.superOnStop this)
    (disable-keep-screen-on this)))

