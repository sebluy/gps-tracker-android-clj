(ns android.sebluy.gpstracker.remote.handlers
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.http :as http]
            [android.sebluy.gpstracker.remote.transitions :as transitions]
            [android.sebluy.gpstracker.remote.logic :as logic]
            [android.sebluy.gpstracker.util :as util]))

(defn receive-response [state response-attrs]
  (transitions/receive-response state response-attrs))

(defn send-request [state request]
  (let [network? (util/network-available? (state :activity))]
    (when network?
      (future
        (let [response-attrs (-> request (logic/request->action) (pr-str) (http/post))]
          (state/handle receive-response response-attrs))))
    (transitions/send-request state network? request)))

(defn retry-request [state]
  (send-request (get-in state [:remote :request]) state))

