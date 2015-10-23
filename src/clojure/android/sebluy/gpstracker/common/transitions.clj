(ns android.sebluy.gpstracker.common.transitions)

;;;; history
(defn create-history [state]
  (assoc state :history '()))

(defn push-history [state page]
  (update state :history conj page))

(defn pop-history [state]
  (update state :history pop))

;;;; pages and navigation
(defn set-page [state page]
  (assoc state :page page))

(defn navigate [state page]
  (-> state
      (push-history (state :page))
      (set-page page)))

(defn back [state]
  (if-let [last-page (first (state :history))]
    (-> state
        (set-page last-page)
        (pop-history))
    state))

;;;; initialization
(defn stash-activity [state activity]
  (assoc state :activity activity))

(defn initialize [state activity]
  (-> state
      (set-page {:id :main})
      (create-history)
      (stash-activity activity)))
