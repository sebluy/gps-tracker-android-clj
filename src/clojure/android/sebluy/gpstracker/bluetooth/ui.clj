(ns android.sebluy.gpstracker.bluetooth.ui
  (:require [android.sebluy.gpstracker.bluetooth.handlers :as handlers]
            [android.sebluy.gpstracker.state :as state]
            [neko.find-view :as find-view])
  (:import [java.util List]
           [android.content Context]
           [android R$layout]
           [android.widget ArrayAdapter
                           ListView
                           AdapterView$OnItemClickListener]))

(def scanning-ui
  [:linear-layout {:orientation :vertical}
   [:text-view {:text "Scanning..."}]
   [:progress-bar {}]
   [:list-view {:id ::list-view}]
   [:button {:text     "Stop"
             :on-click (fn [_] (state/handle handlers/stop-scan))}]])

(def idling-ui
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text     "Start"
             :on-click (fn [_] (state/handle handlers/start-scan))}]])

(defn loading-ui [device-name]
  [:linear-layout {:orientation :vertical}
   [:text-view {:text (str "Loading..." device-name)}]
   [:progress-bar {}]])

(def success-ui
  [:linear-layout {}
   [:text-view {:text "Success"}]])

(defn ui [{{:keys [status device]} :bluetooth}]
  (condp = status
    :scanning scanning-ui
    :idling idling-ui
    :loading (loading-ui device)
    :success success-ui))

;; this list code is most likely reusable (path list)
(defn make-list-click-listener []
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ device-index _]
      ;; what to do on click? multi-method dispatch on mode?
      #_(state/handle start-receiving-path device-index))))

(defn fill-device-list [devices activity]
  (let [[^ListView list-view] (find-view/find-views activity ::list-view)]
    (.setAdapter list-view (ArrayAdapter. ^Context activity ^int R$layout/simple_list_item_1
                                          ^List (or (keys devices) ["No devices"])))
    (.setOnItemClickListener list-view (make-list-click-listener))))

(defn fill [{{:keys [status devices]} :bluetooth activity :activity}]
  (if (#{:idling :scanning} status)
    (fill-device-list devices activity)))
