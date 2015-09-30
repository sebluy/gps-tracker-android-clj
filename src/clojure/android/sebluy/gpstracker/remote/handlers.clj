(ns android.sebluy.gpstracker.remote.handlers
  (:require [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.remote.http :as http]
            [android.sebluy.gpstracker.remote.logic :as logic]
            [android.sebluy.gpstracker.util :as util]
            [neko.activity :as activity]
            [neko.threading :as threading]))

(declare update-ui)

;;;;; Handlers ;;;;;

(defn receive-response [activity [response-code _] old-state]
  (let [new-status (if (= response-code 200) :success :failure)
        new-state (assoc-in old-state [:remote :status] new-status)]
    (update-ui activity new-state)
    new-state))

(defn upload-path [activity old-state]
  (let [network? (util/network-available? activity)
        new-status (if network? :pending :disconnected)
        new-state (assoc-in old-state [:remote :status] new-status)]
    (when network?
      (let [path (get-in old-state [:remote :path])]
        (future
          (let [response-attrs (-> path logic/path->action pr-str http/post)]
            (state/handle receive-response activity response-attrs)))))
    (update-ui activity new-state)
    new-state))

(defn exit-remote-activity [state]
  (dissoc state :remote))

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
             :on-click (fn [_] (state/handle upload-path activity))}]])

(defn render-ui [activity status]
  (threading/on-ui
    (activity/set-content-view!
      activity
      (condp = status
        :success success-ui
        :failure (failure-ui activity "Upload Failed")
        :pending pending-ui
        :disconnected (failure-ui activity "Network Disconnected")))))

(defn update-ui [activity state]
  (render-ui activity (get-in state [:remote :status])))
