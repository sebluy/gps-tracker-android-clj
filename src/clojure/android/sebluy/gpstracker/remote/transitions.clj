(ns android.sebluy.gpstracker.remote.transitions
  (:require [android.sebluy.gpstracker.state :as state]))

(declare update-ui)

;;;;; Handlers ;;;;;

(defmulti action-transition (fn [action _ _] action))

(defmethod action-transition :get-waypoint-paths [_ response-body state]
  ; move to some kind of "global" transition collection
  (assoc state :waypoint-paths response-body))

(defmethod action-transition :default [_ _ state]
  state)

(defn update-status [state status]
  (assoc-in state [:remote :status] status))

(defn receive-response [state [response-code response-body]]
  (if (= response-code 200)
    (let [action (get-in state [:remote :action])]
      (-> (action-transition action response-body state) (update-status :success)))
    (update-status state :failure)))

(defn clear-remote [state]
  (dissoc state :remote))

