(ns android.sebluy.gpstracker.path
  (:require [android.sebluy.gpstracker.util :as util])
  (:import [android.location Location]
           [java.util Date]))

(defn distance-between [[a b]]
  "Computes the distance between two latlngs.
   Result is given in meters."
  (let [results (float-array 1)]
    (Location/distanceBetween (a :latitude) (a :longitude)
                              (b :latitude) (b :longitude) results)
    (aget results 0)))

(defn total-distance [{:keys [points]}]
  "returns total distance in meters of path, where straight line segments
   are used between points"
  (->> points
       (partition 2 1)
       (map distance-between)
       (reduce +)))

(defn waypoint-attributes [path]
  {:created (util/date->string (path :id))
   :distance (format "%.2fm"(total-distance path))
   :count (count (path :points))})
