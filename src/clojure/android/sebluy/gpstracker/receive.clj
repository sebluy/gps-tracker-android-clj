(ns android.sebluy.gpstracker.receive
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [android.sebluy.gpstracker.util :as util]
            [clojure.core.async :as async]
            [android.sebluy.gpstracker.bluetooth-scanner :as scanner]
            [neko.find-view :as find-view])
  (:import [android.content Context Intent]
           [android.bluetooth BluetoothAdapter]
           [android.app Activity]
           [android R$layout]
           [android.widget ArrayAdapter
                           AdapterView$OnItemClickListener]))

(defn make-list-click-listener [activity]
  (reify AdapterView$OnItemClickListener
    (onItemClick [_ _ _ position _]
      #_(swap! state/state assoc :show-path (-> @state/state :paths vals (nth position)))
      #_(util/start-activity activity '.ShowPathActivity))))

(defn scanning-post-render [activity devices]
  (fn []
    (let [[list-view] (find-view/find-views activity ::list-view)]
      (.setAdapter list-view (ArrayAdapter. activity R$layout/simple_list_item_1
                                            (or (keys devices) ["No devices"])))
      (.setOnItemClickListener list-view (make-list-click-listener activity)))))

(defn scanning-ui [chan]
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text "Stop"
             :on-click (fn [_] (async/put! chan :stop))}]])

(defn idle-scan-ui [chan]
  [:linear-layout {:orientation :vertical}
   [:list-view {:id ::list-view}]
   [:button {:text "Start"
             :on-click (fn [_] (async/put! chan :start))}]])

(defn loading-ui []
  [:linear-layout {:orientation :vertical}
   [:text-view {:text "Loading..."}]
   [:progress-bar {}]])

(defn summary-ui []
  [:linear-layout {}
   [:text-view {:text "Success"}]])

(defn render-ui [activity ui post-render-fn]
  (threading/on-ui
    (activity/set-content-view! activity ui)
    (post-render-fn activity)))

(defn get-bluetooth-adapter [activity]
  (.. activity (getSystemService Context/BLUETOOTH_SERVICE) getAdapter))

(defn bluetooth-enabled? [activity]
  (let [bluetooth-adapter (get-bluetooth-adapter activity)]
    (and (some? bluetooth-adapter) (.isEnabled bluetooth-adapter))))

(defn run-render-machine [activity chans]
  (let [chans (assoc chans :button (async/chan))]
    (render-ui activity (scanning-ui (chans :button)) (scanning-post-render activity {}))
    (async/go-loop [devices {}]
      (async/alt!
        (chans :button)
        ([button]
          (condp = button
            :start
            (do (async/put! (chans :control) :start)
                (render-ui activity (scanning-ui (chans :button)) (scanning-post-render activity devices)))
            :stop
            (do (async/put! (chans :control) :stop)
                (render-ui activity (scanning-ui (chans :button)) (scanning-post-render activity devices))))
          (recur devices))
        (chans :status)
        ([status]
          (when (= :stopped status)
            (render-ui activity (idle-scan-ui (chans :control)) (scanning-post-render activity devices)))
          (recur devices))
        (chans :device)
        ([device]
          (let [new-devices (assoc devices (.getName device) device)]
            (render-ui activity (scanning-ui (chans :control)) (scanning-post-render activity new-devices))
            (recur new-devices)))))))

(defn create-chans [& keys]
  (reduce (fn [chans key] (assoc chans key (async/chan))) {} keys))

(activity/defactivity
  android.sebluy.gpstracker.ReceivePathActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (when-not (bluetooth-enabled? this)
      (.startActivityForResult this (Intent. BluetoothAdapter/ACTION_REQUEST_ENABLE) 0))
    (let [chans (create-chans :control :status :device)]
      (scanner/run chans (get-bluetooth-adapter this))
      (run-render-machine this chans)))
  (onStart
    [this]
    (.superOnStart this)
    (util/keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (util/keep-screen-on this false))
  (onActivityResult
    [this _ result _]
    (if (not= result Activity/RESULT_OK)
      (.finish this))))

