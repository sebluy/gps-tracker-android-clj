(ns android.sebluy.gpstracker.remote.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.handlers :as handlers]))

(def pending-ui
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text "Uploading..."}]
   [:progress-bar {}]])

(def success-ui
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text "Upload succeded"}]])

(defn failure-ui [msg]
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text msg}]
   [:button {:text     "Retry"
             :on-click (fn [_] (state/handle handlers/retry-request))}]])

(defn ui [state]
  (let [status (get-in state [:remote :status])]
    (condp = status
      :success success-ui
      :failure (failure-ui "Upload Failed")
      :pending pending-ui
      :disconnected (failure-ui "Network Disconnected"))))
