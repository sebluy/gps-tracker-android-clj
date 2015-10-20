(ns android.sebluy.gpstracker.common.transitions)

(defn set-page [state page]
  (assoc state :page page))

(defn push-history [state page]
  (update state :history conj page))

(defn pop-history [state]
  (update state :history pop))

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
