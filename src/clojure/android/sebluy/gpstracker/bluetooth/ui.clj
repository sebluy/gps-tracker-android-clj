(ns android.sebluy.gpstracker.bluetooth.ui
  (:require [android.sebluy.gpstracker.bluetooth.handlers :as handlers]
            [android.sebluy.gpstracker.bluetooth.util :as util]
            [android.sebluy.gpstracker.state :as state]
            [clojure.string :as s]
            [neko.find-view :as find-view])
  (:import [java.util List]
           [android.content Context]
           [android R$layout]
           [android.widget ArrayAdapter
                           ListView
                           AdapterView$OnItemClickListener]))

(def scanning-ui
  [:linear-layout {:orientation :vertical
                   :gravity :center-horizontal}
   [:text-view {:text "Scanning..."
                :layout-margin-top 50}]
   [:progress-bar {}]
   [:list-view {:id ::list-view
                :layout-margin 50}]])

(def disconnected-ui
  [:linear-layout {:orientation :vertical
                   :gravity :center}
   [:text-view {:text "Bluetooth is disconnected"}]
   [:button {:text     "Retry"
             :padding 30
             :layout-margin 50
             :on-click (fn [_] (state/handle handlers/attempt-scan))}]])

(defn pending-ui [device]
  [:linear-layout {:orientation :vertical
                   :gravity :center}
   [:text-view {:text (str "Pending... " (util/device-key device))}]
   [:progress-bar {}]])

(defn stringify [keyword]
  (-> keyword name s/capitalize))

(defn finished-ui [result]
  [:linear-layout {:gravity :center}
   [:text-view {:text (stringify result)}]])

(defn ui [{{:keys [status device]} :page}]
  (condp = status
    :disconnected disconnected-ui
    :scanning scanning-ui
    :pending (pending-ui device)
    :success (finished-ui status)
    :failure (finished-ui status)))

;; this list code is most likely reusable (path list)
(defn make-list-click-listener [devices]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ device-index _]
      ;; what to do on click? multi-method dispatch on request?
      (state/handle handlers/stop-scan-and-connect (nth (vals devices) device-index)))))

(defn fill-device-list [devices activity]
  (let [[^ListView list-view] (find-view/find-views activity ::list-view)]
    (.setAdapter list-view (ArrayAdapter. ^Context activity ^int R$layout/simple_list_item_1
                                          ^List (or (keys devices) ["No devices"])))
    (.setOnItemClickListener list-view (make-list-click-listener devices))))

(defn fill [{{:keys [status devices]} :page activity :activity}]
  (if (#{:idling :scanning} status)
    (fill-device-list devices activity)))
