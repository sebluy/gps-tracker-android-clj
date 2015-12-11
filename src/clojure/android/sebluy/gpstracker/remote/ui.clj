(ns android.sebluy.gpstracker.remote.ui
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.handlers :as handlers]))

(defn layout [contents]
  (into
   [:linear-layout
    {:orientation :vertical
     :gravity :center}]
   contents))

(def pending-ui
  [[:text-view {:text "Pending..."}]
   [:progress-bar {}]])

(def success-ui
  [[:text-view {:text "Success"}]])

(defn failure-ui [msg]
  [[:text-view {:text msg}]
   [:button {:text "Retry"
             :layout-margin 50
             :padding 30
             :on-click (fn [_] (state/handle handlers/attempt-request))}]])

(defn ui [state]
  (let [status (get-in state [:page :status])]
    (layout
     (condp = status
       :success success-ui
       :failure (failure-ui "Upload Failed")
       :pending pending-ui
       :disconnected (failure-ui "Network Disconnected")))))
