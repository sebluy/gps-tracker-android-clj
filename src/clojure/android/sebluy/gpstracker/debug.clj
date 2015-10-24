(ns android.sebluy.gpstracker.debug)

(def log (atom '()))

(defn push [value]
  (swap! log conj value))

(defn clear []
  (reset! log '()))

; for debuggin purposes
(-> @log first ex-data :value :waypoint-paths)
