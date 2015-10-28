(ns android.sebluy.gpstracker.state
  (:require [android.sebluy.gpstracker.schema :as schema]
            [android.sebluy.gpstracker.debug :as debug]
            [android.sebluy.gpstracker.common.transitions :as transitions]
            [neko.threading :as threading]
            [neko.notify :as notify]))

(defn handle-error [agent exception]
  (threading/on-ui
   (notify/toast "Invalid State... Check debug log"))
  (debug/push exception))

;;(set-validator! state schema/validator)

; don't reload or activity will be lost and ui will still
; be attached to old state
; state is nil until activity is created
(defonce state (agent {}
                      :validator schema/validator
                      :error-handler handle-error))

(defn handle [handler-fn & args]
  "calls handler-fn on state agent with the current value of state
   as the first argument. new state will be result of
   (handler-fn state args)"
  ;; remove this in production
  (send state (fn [state]
                (debug/push state)
                (apply handler-fn state args))))
