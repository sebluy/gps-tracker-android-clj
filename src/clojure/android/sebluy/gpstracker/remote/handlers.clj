(ns android.sebluy.gpstracker.remote.handlers
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.http :as http]
            [android.sebluy.gpstracker.remote.transitions :as transitions]
            [android.sebluy.gpstracker.remote.logic :as logic]
            [android.sebluy.gpstracker.util :as util]
            [neko.activity :as activity]
            [neko.threading :as threading]))

(declare update-ui)

;;;;; Handlers ;;;;;

(defn receive-response [activity response-attrs state]
  (doto (transitions/receive-response state response-attrs)
    (update-ui activity)))

(defn send-action [activity state]
  (let [network? (util/network-available? activity)
        new-status (if network? :pending :disconnected)]
    (when network?
      (future
        (let [response-attrs (-> (logic/build-action state) (pr-str) (http/post))]
          (state/handle receive-response activity response-attrs))))
    (doto (transitions/update-status state new-status)
      (update-ui activity))))

(defn exit-remote-activity [state]
  (transitions/clear-remote state))

;;;;; UI ;;;;;

(def pending-ui
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text "Uploading..."}]
   [:progress-bar {}]])

(def success-ui
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text "Upload succeded"}]])

(defn failure-ui [activity msg]
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text msg}]
   [:button {:text     "Retry"
             :on-click (fn [_] (state/handle send-action activity))}]])

(defn render-ui [activity status]
  (threading/on-ui
    (activity/set-content-view!
      activity
      (condp = status
        :success success-ui
        :failure (failure-ui activity "Upload Failed")
        :pending pending-ui
        :disconnected (failure-ui activity "Network Disconnected")))))

(defn update-ui [state activity]
  (render-ui activity (get-in state [:remote :status])))
