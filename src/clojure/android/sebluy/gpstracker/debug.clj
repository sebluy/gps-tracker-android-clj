(ns android.sebluy.gpstracker.debug)

(def log (atom '()))

(defn push [value]
  (swap! log conj value))

(defn clear []
  (reset! log '()))
