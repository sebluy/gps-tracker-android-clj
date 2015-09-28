(ns android.sebluy.gpstracker.state)

(def state (atom {}))

(defn handle [handler-fn & args]
  (swap! state (apply partial handler-fn args)))

(handle set-sum 2 3)

(defn set-sum [x y state]
  (assoc state :sum (+ x y)))

