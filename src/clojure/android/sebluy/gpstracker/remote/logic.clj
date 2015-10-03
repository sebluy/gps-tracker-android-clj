(ns android.sebluy.gpstracker.remote.logic)

(defn path->action [path]
  [[:add-path
    (->> (path :points)
         (map (fn [point]
                (select-keys point #{:latitude :longitude :speed :accuracy})))
         (into []))]])

(defmulti build-action (fn [state] (get-in state [:remote :action])))

(defmethod build-action :upload-path [state]
  (let [path (get-in state [:remote :path])]
    (path->action path)))

(defmethod build-action :get-waypoint-paths [_]
  [[:get-waypoint-paths]])
