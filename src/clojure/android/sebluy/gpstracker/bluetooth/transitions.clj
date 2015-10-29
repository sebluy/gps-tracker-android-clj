(ns android.sebluy.gpstracker.bluetooth.transitions
  (:require [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.common.transitions :as common-transitions]
            [android.sebluy.gpstracker.bluetooth.util :as util]))

(defn add-path [state path]
  (assoc-in state [:paths (path :created-at)] path))

(defn add-value-to-receiving-path [state value]
  (update-in state [:bluetooth :values] conj value))

(defn finish-receiving-path [state]
  (let [path-values (get-in state [:bluetooth :values])]
    (-> state
        (add-path (path/make-new (path/parse-path path-values)))
        (update :bluetooth
                (fn [bluetooth]
                  (-> bluetooth
                      (dissoc :values)
                      (assoc :status :success)))))))

(defn receive-path-value [state value]
  (let [intermediate (add-value-to-receiving-path state value)]
    (if (= value "finish")
      (finish-receiving-path intermediate)
      intermediate)))

(defn start-receiving-path [state device-name]
  (update state :bluetooth
          (fn [bluetooth]
            (-> bluetooth
                (assoc :status :receiving
                       :device device-name
                       :values [])
                (dissoc :scanner)))))

(defn add-device [state device]
  (let [key (util/device-key device)]
    (assoc-in state [:page :devices key] device)))

(defn stop-scan [state]
  (update state :page
          (fn [page]
            (-> page
                (dissoc :scanner)
                (assoc :status :idling)))))

(defn start-scan [state bluetooth-map]
  (update state :page merge bluetooth-map {:status :scanning}))

(defn initialize [state bluetooth-map]
  (common-transitions/navigate state (merge {:id :bluetooth} bluetooth-map)))
