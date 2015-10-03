(ns android.sebluy.gpstracker.bluetooth.transitions
  (:require [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.bluetooth.logic :as logic]))

(defn add-path [old-state path]
  (assoc-in old-state [:paths (path :created-at)] path))

(defn add-value-to-receiving-path [old-state value]
  (update-in old-state [:bluetooth :values] conj value))

(defn finish-receiving-path [old-state]
  (-> old-state
      (add-path (path/make-new (path/parse-path (get-in old-state [:bluetooth :values]))))
      (update :bluetooth #(-> % (dissoc :values) (assoc :status :success)))))

(defn receive-path-value [old-state value]
  (let [intermediate (add-value-to-receiving-path old-state value)]
    (if (= value "finish")
      (finish-receiving-path intermediate)
      intermediate)))

(defn start-receiving-path [old-state device-name]
  (update old-state :bluetooth
          (fn [bluetooth]
            (-> bluetooth
                (assoc :status :receiving
                       :device device-name
                       :values [])
                (dissoc :scanner)))))

(defn add-device [old-state device]
  (let [key (logic/device-key device)]
    (assoc-in old-state [:bluetooth :devices key] device)))

(defn stop-scan [old-state]
  (update old-state :bluetooth
          (fn [bluetooth]
            (-> bluetooth
                (dissoc :scanner)
                (assoc :status :idling)))))

(defn start-scan [state devices scanner]
  (update state :bluetooth assoc
          :scanner scanner
          :status :scanning
          :devices devices))

