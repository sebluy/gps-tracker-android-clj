(ns android.sebluy.gpstracker.state)

(def state (atom {}))

(defn handle [handler-fn & args]
  (swap! state (apply partial handler-fn args)))

