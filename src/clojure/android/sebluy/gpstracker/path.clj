(ns android.sebluy.gpstracker.path
  (:import [android.location Location]))

(defn location->point [location]
  (merge {:latitude  (.getLatitude location)
          :longitude (.getLongitude location)
          :time (.getTime location)}
         (if (.hasSpeed location)
           {:speed (.getSpeed location)})
         (if (.hasAccuracy location)
           {:accuracy (.getAccuracy location)})))

(defn make-new []
  {:points            []
   :speed-acum        0.0
   :points-with-speed 0
   :total-distance    0.0})

(defn distance-between [a b]
  (let [results (float-array 1)]
    (Location/distanceBetween (a :latitude) (a :longitude)
                              (b :latitude) (b :longitude) results)
    (aget results 0)))

(defn add-point [path point]
  (-> path
      (update :points conj point)
      (update :speed-acum + (or (point :speed) 0.0))
      (update :points-with-speed + (if (point :speed) 1 0))
      (update :total-distance + (if (seq (path :points))
                                  (distance-between (last (path :points)) point)
                                  0))))

(defn current-speed [path]
  (-> path :points last :speed))

(defn average-speed [path]
  (if (> (path :points-with-speed) 0)
    (/ (path :speed-acum) (path :points-with-speed))
    0.0))

(defn time-elapsed [path]
  (let [points (path :points)]
    (if (>= (count points) 2)
      (- (-> points last :time)
         (-> points first :time))
      0)))
