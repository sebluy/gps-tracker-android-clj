(ns android.sebluy.gpstracker.show-waypoint-path.ui)

(defn ui [state]
  (let [path (get-in state [:page :path])]
    [:linear-layout {:orientation :vertical}
     [:text-view {:text (str "Path length: " (count path))}]]))
