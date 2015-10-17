(comment
  (ns android.sebluy.gpstracker.tracking
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [clojure.core.async :as async]
            [android.sebluy.gpstracker.ui :as ui]
            [android.sebluy.gpstracker.gps :as gps]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.util :as util]))

(defn get-chan [activity key]
  (get-in @(activity/get-state activity) [:channels key]))

(defn put-chan [activity key message]
  (async/put! (get-chan activity key) message))

(defn control-button [activity text message]
  [:button {:text text :on-click (fn [_] (put-chan activity :control message))}])

(defn pause-resume-button [activity attributes]
  (condp = (attributes :status)
    :tracking
    (control-button activity "Pause" :stop)
    :disconnected
    (control-button activity "Resume" :start)
    nil))

(defn finish-button [activity]
  [:button {:text     "Finish"
            :on-click (fn [_] (.finish activity))}])

(defn button-ui [activity attributes]
  [:linear-layout {}
   (pause-resume-button activity attributes)
   (finish-button activity)])

(defn ui [activity attributes]
  [:linear-layout {:orientation :vertical}
   (ui/table attributes)
   (button-ui activity attributes)])

(defn render-ui [activity attributes]
  (threading/on-ui
    (activity/set-content-view! activity (ui activity attributes))))

(defn run-render-machine [activity status-chan location-chan path-id]
  (let [attributes (atom {})]
    (async/go-loop []
      (swap! attributes assoc :status (async/<! status-chan))
      (render-ui activity @attributes)
      (recur))
    (async/go-loop []
      (let [location (async/<! location-chan)]
        (swap! state/state update-in [:paths path-id] #(path/add-point % (path/location->point location))))
      (let [path (get-in @state/state [:paths path-id])]
        (swap! attributes merge (path/attributes path))
        (render-ui activity @attributes))
      (recur))))

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
      (render-ui this {:status :disconnected})))
  (onStart
    [this]
    (.superOnStart this)
    (async/put! (get-chan this :control) :start)
    (util/keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (async/put! (get-chan this :control) :stop)
    (util/keep-screen-on this false))))

