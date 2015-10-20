(ns android.sebluy.gpstracker.path
  (:import [android.location Location]
           [java.util Date]))

(defn location->point [location]
  (merge {:latitude  (.getLatitude location)
          :longitude (.getLongitude location)
          :time      (.getTime location)}
         (if (.hasSpeed location)
           {:speed (.getSpeed location)})
         (if (.hasAccuracy location)
           {:accuracy (.getAccuracy location)})))

(defn distance-between [[a b]]
  (let [results (float-array 1)]
    (Location/distanceBetween (a :latitude) (a :longitude)
                              (b :latitude) (b :longitude) results)
    (aget results 0)))

(defn total-distance [path]
  "returns total distance in meters of path, where straight line segments
   are used between points"
  (first
   (reduce (fn [[sum last-point] next-point]
             (let [new-sum (+ sum (distance-between last-point next-point))]
               [new-sum next-point]))
           [0 (first path)]
           (rest path))))

(defn total-distance [path]
  "returns total distance in meters of path, where straight line segments
   are used between points"
  (->> path
       (partition 2 1)
       (map distance-between)
       (reduce +)))

; might not need
#_(defn add-point [path point]
  (-> path
      (update :points conj point)
      (update :speed-acum + (or (point :speed) 0.0))
      (update :points-with-speed + (if (point :speed) 1 0))
      (update :total-distance + (if (seq (path :points))
                                  (distance-between (last (path :points)) point)
                                  0))))

; might not need
(defn make-new
  ([] {:created-at        (Date.)
       :points            []
       :speed-acum        0.0
       :points-with-speed 0
       :total-distance    0.0})
  ([points]
   (reduce add-point (make-new) points)))

; might not need
(defn current-speed [path]
  (-> path :points last :speed))

; might not need
(defn average-speed [path]
  (if (> (path :points-with-speed) 0)
    (/ (path :speed-acum) (path :points-with-speed))
    0.0))

; might not need
(defn time-elapsed [path]
  (let [points (path :points)]
    (if (>= (count points) 2)
      (- (-> points last :time)
         (-> points first :time))
      0)))

(defn parse-path [raw-path]
  "parses a path from a ghetto rigged serialization format used
  for bluetooth communication"
  (if (not= ["start" "finish"] [(first raw-path) (last raw-path)])
    :error
    (mapv (fn [[latitude longitude speed]]
        {:latitude latitude :longitude longitude :speed speed})
        (partition 3 3 nil (map #(Double/valueOf %) (pop (into [] (rest raw-path))))))))

(defn waypoint-attributes [path]
  {:total-distance (format "%.2fm"(total-distance path))})

(defn attributes [path]
  {:current-speed  (current-speed path)
   :average-speed  (average-speed path)
;   :time-elapsed   (/ (time-elapsed path) 1000.0 60.0)
   :total-distance (path :total-distance)})
