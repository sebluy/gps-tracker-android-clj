(ns android.sebluy.gpstracker.main.transitions
  (:require [android.sebluy.gpstracker.common.transitions :as common-transitions]))

(defn stash-activity [state activity]
  (assoc state :activity activity))

(defn create-history [state]
  (assoc state :history '()))

(defn initialize [_ activity]
  (-> {}
      (common-transitions/set-page {:id :main})
      (create-history)
      (stash-activity activity)))
