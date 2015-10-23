(ns android.sebluy.gpstracker.state
  (:require [android.sebluy.gpstracker.schema :as schema]
            [android.sebluy.gpstracker.debug :as debug]
            [android.sebluy.gpstracker.common.transitions :as transitions]
            [android.sebluy.gpstracker.ui :as ui]
            [neko.threading :as threading]
            [neko.notify :as notify]))

(defn error-handler [agent exception]
  (threading/on-ui
   (notify/toast "Invalid State... Check debug log"))
  (debug/push exception))

; may cause circular dependencies but works for now
(defn initialize [activity]
  "Create agent and attach activity along with startup state"
  (let [initial-state (transitions/initialize {} activity)]
    (send state (fn [_] initial-state))
    (set-error-handler! state error-handler)
    (set-error-mode! state :continue)
    (set-validator! state schema/validator)
    (add-watch state :ui ui/render-ui)))

; don't reload or activity will be lost and ui will still
; be attached to old state
(defonce state (agent nil))

(defn handle [handler-fn & args]
  "calls handler-fn on state agent with the current value of state
   as the first argument. new state will be result of
   (handler-fn state args)"
  (send state (fn [state] (apply handler-fn state args))))
