(ns android.sebluy.gpstracker.receive
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [android.sebluy.gpstracker.util :as util]
            [android.sebluy.gpstracker.bluetooth-scanner :as scanner]
            [android.sebluy.gpstracker.bluetooth-loader :as loader]
            [neko.find-view :as find-view]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.path :as path])
  (:import [android.bluetooth BluetoothDevice]
           [android.os Handler]))

(declare update-ui)

;;;;; Handlers ;;;;;
(defn receive-value [activity value state]
  ;;; move this to transitions
  (let [new-state
        (let [appended-state (update-in state [:bluetooth :values] conj value)]
          (if (= value "finish")
            (-> appended-state
                (add-path (path/make-new (path/parse-path (get-in appended-state [:bluetooth :values]))))
                (update :bluetooth #(-> % (dissoc :values) (assoc :status :success))))
            appended-state))]
    (update-ui activity new-state)
    new-state))

(defn start-receiving-path [activity device-index state]
  (let [device (-> state (get-in [:bluetooth :devices]) vals (nth device-index))
        device-name (.getName ^BluetoothDevice device)]
    (stop-scan activity)
    (loader/new-serial-bluetooth
      activity device identity
      (fn [value] (state/handle receive-value activity value)))
    (render-ui activity (loading-ui device-name) identity)
    (update state :bluetooth assoc
            :status :receiving
            :device device-name
            :values [])))

(defn add-device [activity device state]
  (let [key (device-key device)]
    (render-ui activity (scanning-ui activity) (fill-device-list (get-in state [:bluetooth :devices])))
    (assoc-in state [:bluetooth :devices key] device)))

(defn start-scan [activity state]
  (let [adapter (util/get-bluetooth-adapter activity)
        devices (get-bonded-devices adapter)
        scanner (scanner/start-scan adapter (fn [device] (state/handle add-device activity device)))]
    (render-ui activity (scanning-ui activity) (fill-device-list devices))
    (.postDelayed (Handler.) (state/handle stop-scan activity) 5000)
    (update state :bluetooth assoc :scanner scanner :status :scanning :devices devices)))

(defn stop-scan [activity state]
  (if (= get-in state [:bluetooth :status] :scanning)
    (do (scanner/stop-scan (util/get-bluetooth-adapter activity) (get-in state [:bluetooth :scanner]))
        (render-ui activity (idle-scan-ui activity) (fill-device-list (state :devices)))
        (update state :bluetooth #(-> % (dissoc :scanner) (assoc :status :idling))))
    state))

;;;; UI ;;;;;

(defn scanning-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:text-view {:text "Scanning..."}]
   [:progress-bar {}]
   [:list-view {:id ::list-view}]
   [:button {:text     "Stop"
             :on-click (fn [_] (state/handle stop-scan activity))}]])

(defn idle-scan-ui [activity]
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text     "Start"
             :on-click (fn [_] (state/handle start-scan activity))}]])

(defn loading-ui [device-name]
  [:linear-layout {:orientation :vertical}
   [:text-view {:text (str "Loading..." device-name)}]
   [:progress-bar {}]])

(defn summary-ui []
  [:linear-layout {}
   [:text-view {:text "Success"}]])

(defn render-ui [activity ui post-render-fn]
  (threading/on-ui
    (activity/set-content-view! activity ui)
    (post-render-fn activity)))

(defn make-list-click-listener [activity]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ device-index _]
      (state/handle start-receiving-path activity device-index))))

(defn fill-device-list [devices]
  (fn [activity]
    (let [[^ListView list-view] (find-view/find-views activity ::list-view)]
      (.setAdapter list-view (ArrayAdapter. ^Context activity ^int R$layout/simple_list_item_1
                                            ^List (or (keys devices) ["No devices"])))
      (.setOnItemClickListener list-view (make-list-click-listener activity)))))

(defn update-ui [activity state]
  (condp = (get-in state [:bluetooth :status])
    :receiving (render-ui activity (loading-ui (get-in state [:bluetooth :device])) identity)
    :idling (render-ui activity (idle-scan-ui activity) (fill-device-list (get-in state [:bluetooth :devices])))
    :success (render-ui activity (summary-ui) identity)
    :scanning (render-ui activity (scanning-ui activity) (fill-device-list (get-in state [:bluetooth :devices])))))

