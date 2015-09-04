(ns android.sebluy.gpstracker.tracking
  (:require [neko.activity :as activity]
            [neko.ui.mapping :as mapping]
            [neko.threading :as threading]
            [clojure.core.async :as async]
            [android.sebluy.gpstracker.gps :as gps]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.path :as path])
  (:import [android.widget TableLayout
                           TableRow]
           [android.view WindowManager$LayoutParams]))

(def attributes (atom {}))

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

(defn table [attributes]
  (into [:table-layout {}]
        (map (partial apply table-row)
             attributes)))

(defn get-chan [activity key]
  (get-in @(activity/get-state activity) [:channels key]))

(defn put-chan [activity key message]
  (async/put! (get-chan activity key) message))

(defn control-button [activity text message]
  [:button {:text text :on-click (fn [_] (put-chan activity :control message))}])

(defn pause-resume-button [activity]
   (condp = (@attributes :status)
    :tracking
    (control-button activity "Pause" :stop)
    :disconnected
    (control-button activity "Resume" :start)
    nil))

(defn finish-button [activity]
  [:button {:text     "Finish"
            :on-click (fn [_] (.finish activity))}])

(defn button-ui [activity]
  [:linear-layout {}
   (pause-resume-button activity)
   (finish-button activity)])

(defn ui [activity]
  [:linear-layout {}
   (table @attributes)
   (button-ui activity)])

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view! activity (ui activity))))

(defn keep-screen-on [activity boolean]
  (threading/on-ui
    (let [window (.getWindow activity)]
      (if boolean
        (.addFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)
        (.clearFlags window WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON)))))

(defn run-render-machine [activity status-chan location-chan path-id]
  (async/go-loop []
    (swap! attributes assoc :status (async/<! status-chan))
    (render-ui activity)
    (recur))
  (async/go-loop []
    (let [location (async/<! location-chan)]
      (swap! state/state update-in [:paths path-id] #(path/add-point % (path/location->point location)))
      (let [path (get-in @state/state [:paths path-id])]
        (swap! attributes assoc
               :current-speed (path/current-speed path)
               :average-speed (path/average-speed path)
               :time-elapsed (/ (path/time-elapsed path) 1000.0 60.0)
               :total-distance (path :total-distance))))
    (render-ui activity)
    (recur)))

(activity/defactivity
  android.sebluy.gpstracker.TrackingActivity
  :key :tracking
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (let [new-path (path/make-new)
          path-id (new-path :created-at)]
      (swap! state/state assoc-in [:paths path-id] new-path)
      (let [[status-chan control-chan location-chan] (gps/run-location-machine this)]
        (run-render-machine this status-chan location-chan path-id)
        (reset! (activity/get-state this)
                {:channels {:status status-chan :control control-chan :location location-chan}}))
      (render-ui this)))
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

