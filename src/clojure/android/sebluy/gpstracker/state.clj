(ns android.sebluy.gpstracker.state
  (:require [android.sebluy.gpstracker.schema :as schema]
            [neko.threading :as threading]
            [neko.notify :as notify]))

(defn handle-error [agent exception]
  (threading/on-ui
   (notify/toast "Something went wrong...")))

; don't reload or activity will be lost and ui will still
; be attached to old state
; state is nil until activity is created
(defonce state (agent {}
                      :validator schema/validator
                      :error-handler handle-error))

(defn handle [handler-fn & args]
  "Calls handler-fn on state agent with the current value of state
   as the first argument. New state will be result of
   (handler-fn state args)"
  (send state (fn [state]
                (apply handler-fn state args))))
