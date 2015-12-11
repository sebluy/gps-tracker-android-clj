(ns android.sebluy.gpstracker.remote.transitions
  (:require [clojure.edn :as edn]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]))

(defn initialize [state request]
  (common-transitions/navigate state {:id :remote
                                      :request request
                                      :status :disconnected}))

(defmulti request-transition (fn [state request response-body] (request :action)))

(defmethod request-transition :get-paths [state _ response-body]
  (assoc state :waypoint-paths (first (edn/read-string response-body))))

(defmethod request-transition :default [state _ _]
  state)

(defn send-request [state future]
  (update state :page assoc
          :status :pending
          :future future))

(defn receive-response [state [response-code response-body]]
  (let [success (= response-code 200)
        status (if success :success :failure)
        request (get-in state [:page :request])]
    (-> state
        (cond-> success (request-transition request response-body))
        (update :page (fn [page] (-> page
                                     (assoc :status status)
                                     (dissoc :future)))))))

(defn attempt-receive-response [state response-attrs]
  (cond-> state
    (= (get-in state [:page :id]) :remote)
    (receive-response response-attrs)))
