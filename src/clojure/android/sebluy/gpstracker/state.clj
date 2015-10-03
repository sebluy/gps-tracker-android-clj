(ns android.sebluy.gpstracker.state)

(def state (agent {}))

(defn handle [handler-fn & args]
  (send state (apply partial handler-fn args)))

