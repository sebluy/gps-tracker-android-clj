(ns android.sebluy.gpstracker.remote.transitions
  (:require [clojure.edn :as edn]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]))

(defmulti request-transition (fn [action _ _] action))

(defmethod request-transition :get-waypoint-paths [_ response-body state]
  (assoc state :waypoint-paths (first (edn/read-string response-body))))

(defmethod request-transition :default [_ _ state]
  state)

(defn update-request [state request]
  (assoc-in state [:page :request] request))

(defn update-status [state status]
  (assoc-in state [:page :status] status))

(defn update-request-and-status [state sent? request]
  (-> state
      (update-request request)
      (update-status (if sent? :pending :disconnected))))

(defn send-request [state sent? request]
  (-> state
      (common-transitions/navigate {:id :remote})
      (update-request-and-status sent? request)))

(defn receive-response [state [response-code response-body]]
  (if (= response-code 200)
    (let [request (get-in state [:page :request])]
      (-> (request-transition request response-body state) (update-status :success)))
    (update-status state :failure)))
