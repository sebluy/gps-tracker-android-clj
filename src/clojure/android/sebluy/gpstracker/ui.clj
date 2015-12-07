(ns android.sebluy.gpstracker.ui
  (:require [android.sebluy.gpstracker.waypoint-path-list.ui :as waypoint-path-list-ui]
            [android.sebluy.gpstracker.show-waypoint-path.ui :as show-waypoint-path-ui]
            [android.sebluy.gpstracker.remote.ui :as remote-ui]
            [android.sebluy.gpstracker.bluetooth.ui :as bluetooth-ui]
            [android.sebluy.gpstracker.state :as state]
            [neko.ui.mapping :as mapping]
            [neko.threading :as threading]
            [neko.activity :as activity]
            [clojure.string :as string])
  (:import [android.widget TableLayout
                           TableRow]))

(def default-ui
  [:linear-layout {}
   [:text-view
    {:text "Nothing's here..."}]])

(defn ui [state]
  "Given a state returns a data structure representing
   the UI"
  (condp = (get-in state [:page :id])
    :waypoint-path-list (waypoint-path-list-ui/ui state)
    :show-waypoint-path (show-waypoint-path-ui/ui state)
    :bluetooth (bluetooth-ui/ui state)
    :remote (remote-ui/ui state)
    default-ui))

(defn fill [state]
  "After initial state render, fill in ui with extras (populate lists...)"
  (condp = (get-in state [:page :id])
    :waypoint-path-list (waypoint-path-list-ui/fill state)
    :bluetooth (bluetooth-ui/fill state)
    identity))

(defn render-ui [_ _ _ new-state]
  "renders a new ui representing the current state"
  (when-let [activity (new-state :activity)]
    (threading/on-ui
      (activity/set-content-view! activity (ui new-state))
      (fill new-state))))

; render ui on state changes
(add-watch state/state :ui render-ui)
