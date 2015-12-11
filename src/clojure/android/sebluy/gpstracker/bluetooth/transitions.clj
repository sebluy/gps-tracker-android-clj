(ns android.sebluy.gpstracker.bluetooth.transitions
  (:require [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]))

;; helper
(defn serialize-path [{:keys [points]}]
  "Create a list of strings starting with waypoint count followed by each
   waypoint field in format '(count lat-1 lng-1 lat-2 lng-2 ...).
   e.g. '(3 1.034 2.199 1.035 2.200 3.994 5.947) with each value as a string."
  (cons (-> points count str) (->> points (map vals) flatten (map str))))

;; page initialization
(defn initialize [state bluetooth-map]
  (common-transitions/navigate state (merge {:id :bluetooth} bluetooth-map)))

;; scanning
(defn add-device [state device]
  (let [key (util/device-key device)]
    (assoc-in state [:page :devices key] device)))

(defn stop-scan [state]
  (update state :page
          (fn [page]
            (-> page
                (dissoc :scanner)
                (assoc :status :idling)))))

(defn start-scan [state scanner]
  (update state :page assoc
          :status :scanning
          :scanner scanner))

;; connect
(defn connect [state device loader]
  (let [path (get-in state [:page :request :path])]
    (update state :page assoc
            :loader loader
            :status :pending
            :device device
            :write-queue (serialize-path path))))

;; loading stage
(defn pop-write-queue [state]
  (update-in state [:page :write-queue] (fn [queue] (drop 1 queue))))

;; disconnect
(defn disconnect [state]
  (let [write-queue (get-in state [:page :write-queue])
        result (if (empty? write-queue) :success :failure)]
    (update state :page
            (fn [page] (-> page
                           (dissoc :loader)
                           (dissoc :device)
                           (dissoc :write-queue)
                           (assoc :status result))))))
