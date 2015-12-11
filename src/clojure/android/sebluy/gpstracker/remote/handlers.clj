(ns android.sebluy.gpstracker.remote.handlers
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.http :as http]
            [android.sebluy.gpstracker.remote.transitions :as transitions]
            [android.sebluy.gpstracker.util :as util]))

(declare attempt-request)

(defn initialize [state request]
  (-> state
      (transitions/initialize request)
      (attempt-request)))

(defn make-future [request]
  (future
    (let [response-attrs (-> [request] (pr-str) (http/post))]
      (state/handle transitions/attempt-receive-response response-attrs))))

(defn send-request [state]
  (let [future (make-future (get-in state [:page :request]))]
    (transitions/send-request state future)))

(defn attempt-request [state]
  (cond-> state
    (util/network-available? (state :activity))
    (send-request)))

(defn cleanup [state]
  (when-let [future (get-in state [:page :future])]
    (future-cancel future)))
