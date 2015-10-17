(ns android.sebluy.gpstracker.state)

; don't reload or activity will be lost and ui will still be attached to old state
(defonce state (agent {}))

(defn handle [handler-fn & args]
  (send state (fn [state] (apply handler-fn state args))))

