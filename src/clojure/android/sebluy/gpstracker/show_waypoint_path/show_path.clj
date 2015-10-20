(ns android.sebluy.gpstracker.show-path
  (:require [neko.activity :as activity]
            [neko.threading :as threading]
            [android.sebluy.gpstracker.state :as state]
            [android.sebluy.gpstracker.ui :as ui]
            [android.sebluy.gpstracker.path :as path]
            [android.sebluy.gpstracker.util :as util]))

(defn render-ui [activity path]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout {:orientation :vertical}
       [:text-view {:text (str (path :created-at))}]
       (ui/table (path/attributes path))
       [:button
        {:text     "Upload"
         :on-click (fn [_]
                     (swap! state/state assoc-in [:remote :path] path)
                     (util/start-activity activity '.RemoteActivity))}]])))
