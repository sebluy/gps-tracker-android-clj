(ns android.sebluy.gpstracker.state)

; don't reload or activity will be lost and ui will still
; be attached to old state
(defonce state (agent {}))

(defn handle [handler-fn & args]
  "calls handler-fn on state agent with the current value of state
   as the first argument. new state will be result of
   (handler-fn state args)"
  (send state (fn [state] (apply handler-fn state args))))
