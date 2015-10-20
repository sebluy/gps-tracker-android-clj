(ns android.sebluy.gpstracker.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.main.ui :as main-ui]
            [android.sebluy.gpstracker.waypoint-path-list.ui :as waypoint-path-list-ui]
            [android.sebluy.gpstracker.show-waypoint-path.ui :as show-waypoint-path]
            [android.sebluy.gpstracker.remote.ui :as remote-ui]
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
  "given a state returns a data structure representing
   the ui"
  (condp = (get-in state [:page :id])
    :main (main-ui/ui state)
    :waypoint-path-list waypoint-path-list-ui/ui
    :show-waypoint-path (show-waypoint-path/ui state)
    :remote (remote-ui/ui state)
    default-ui))

(defn fill [state activity]
  "after initial state render, fill in ui with extras (populate lists...)"
  (condp = (get-in state [:page :id])
    :waypoint-path-list (waypoint-path-list-ui/fill state activity)
    identity))

(defn render-ui [_ _ _ new-state]
  "renders a new ui representing the current state"
  (when-let [activity (new-state :activity)]
    (threading/on-ui
      (activity/set-content-view! activity (ui new-state))
      (fill new-state activity))))

; add "observer" to render on state change
(add-watch state/state :ui render-ui)

; utils (add to different namespace?)

(mapping/defelement
  :table-layout
  :classname TableLayout
  :inherits :view-group)

(mapping/defelement
  :table-row
  :classname TableRow
  :inherits :view)

(defn table-row [value]
  [:table-row {}
   [:text-view {:text value}]])

(defn keyword->title [keyword]
  (-> :total-distance
      (name)
      (string/split #"-")
      (->> (map string/capitalize)
           (string/join " "))))

(defn readable-attribute [[key value]]
  [(keyword->title key) (str value)])

(defn table [attributes]
  (into [:table-layout {}]
        (map table-row
             (mapcat readable-attribute attributes))))
