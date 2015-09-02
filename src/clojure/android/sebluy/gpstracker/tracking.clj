(ns android.sebluy.gpstracker.tracking
  (:require [neko.activity :as activity]
            [neko.ui.mapping :as mapping]
            [neko.threading :as threading]
            [clojure.core.async :as async]
            [android.sebluy.gpstracker.gps :as gps]
            [android.sebluy.gpstracker.state :as state])
  (:import [android.widget TableLayout
                           TableRow]
           [android.view WindowManager$LayoutParams]))

(def attributes (atom nil))

(mapping/defelement
  :table-layout
  :classname TableLayout
  :inherits :view-group)

(mapping/defelement
  :table-row
  :classname TableRow
  :inherits :view)

(defn table-row [title value]
  [:table-row {}
   [:text-view {:text (str title)}]
   [:text-view {:text (str value)}]])

(defn tracking-table [attributes]
  (into [:table-layout {}]
        (map (partial apply table-row)
             attributes)))

(defn ui [activity]
  [:linear-layout {}
   (tracking-table @attributes)
   [:button {:text "Pause"}]
   [:button {:text     "Finish"
             :on-click (fn [_] (.finish activity))}]])

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view! activity (ui activity))))

(defn keep-screen-on [activity boolean]
  (threading/on-ui
    (let [window (.getWindow activity)]
      (if boolean
        (.addFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)
        (.clearFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)))))

(defn get-chan [activity key]
  (get-in @(activity/get-state activity) [:channels key]))

(defn location->map [location]
  (merge {:latitude  (.getLatitude location)
          :longitude (.getLongitude location)}
         (if (.hasSpeed location)
           {:speed (.getSpeed location)})
         (if (.hasAccuracy location)
           {:accuracy (.getAccuracy location)})))

(defn run-render-machine [activity status-chan location-chan]
  (async/go-loop []
    (swap! attributes assoc :status (async/<! status-chan))
    (render-ui activity)
    (recur))
  (async/go-loop []
    (let [location (async/<! location-chan)]
      (swap! state/state update :path #(conj % (location->map location)))
      (swap! attributes assoc
             :latitude (.getLatitude location)
             :longitude (.getLongitude location)
             :speed (.getSpeed location)
             :accuracy (.getAccuracy location)))
    (render-ui activity)
    (recur)))

(activity/defactivity
  android.sebluy.gpstracker.TrackingActivity
  :key :tracking
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (swap! state/state assoc :path [])
    (reset! attributes {:status :pending})
    (let [[status-chan control-chan location-chan] (gps/run-location-machine this)]
      (run-render-machine this status-chan location-chan)
      (reset! (activity/get-state this)
              {:channels {:status status-chan :control control-chan :location location-chan}}))
    (render-ui this))
  (onStart
    [this]
    (.superOnStart this)
    (async/put! (get-chan this :control) :start)
    (keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (async/put! (get-chan this :control) :stop)
    (keep-screen-on this false)))

