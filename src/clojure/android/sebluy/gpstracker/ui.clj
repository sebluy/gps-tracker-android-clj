(ns android.sebluy.gpstracker.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.main.ui :as main-ui]
            [android.sebluy.gpstracker.waypoint-path-list.ui :as waypoint-path-list-ui]
            [android.sebluy.gpstracker.remote.ui :as remote-ui]
            [neko.ui.mapping :as mapping]
            [neko.threading :as threading]
            [neko.activity :as activity])
  (:import [android.widget TableLayout
                           TableRow]))

(def default-ui
  [:linear-layout {}
   [:text-view
    {:text "Nothing's here..."}]])

(defn ui [state]
  (condp = (state :page)
    :main (main-ui/ui state)
    :waypoint-path-list waypoint-path-list-ui/ui
    :remote (remote-ui/ui state)
    default-ui))

(defn fill [state activity]
  (condp = (state :page)
    :waypoint-path-list (waypoint-path-list-ui/fill state activity)
    identity))

(defn render-ui [_ _ _ new-state]
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

(defn table-row [title value]
  [:table-row {}
   [:text-view {:text (str title)}]
   [:text-view {:text (str value)}]])

(defn table [attributes]
  (into [:table-layout {}]
        (map (partial apply table-row)
             attributes)))

