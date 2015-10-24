(ns android.sebluy.gpstracker.remote.logic)

(defn path->action [path]
  [[:add-path
    (->> (path :points)
         (map (fn [point]
                (select-keys point #{:latitude :longitude :speed :accuracy})))
         (into []))]])

(defn request->action [request]
  (condp = request
    :get-waypoint-paths [[:get-waypoint-paths]]))
