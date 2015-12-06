(ns android.sebluy.gpstracker.remote.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.handlers :as handlers]))

(def pending-ui
  [:linear-layout
   {:orientation :vertical
    :gravity :center}
   [:text-view {:text "Pending..."}]
   [:progress-bar {}]])

(def success-ui
  [:linear-layout
   {:orientation :vertical
    :gravity :center}
   [:text-view {:text "Success"}]])

(defn failure-ui [msg]
  [:linear-layout
   {:orientation :vertical
    :gravity :center}
   [:text-view {:text msg}]
   [:button {:text     "Retry"
             :on-click (fn [_] (state/handle handlers/retry-request))}]])

(defn ui [state]
  (let [status (get-in state [:page :status])]
    (condp = status
      :success success-ui
      :failure (failure-ui "Upload Failed")
      :pending pending-ui
      :disconnected (failure-ui "Network Disconnected"))))
