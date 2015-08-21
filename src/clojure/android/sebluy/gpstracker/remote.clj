(ns android.sebluy.gpstracker.remote
  (:require [neko.activity :as activity]
            [neko.threading :as threading])
  (:import java.net.URL
           java.io.BufferedOutputStream
           android.content.Context))

(declare upload-path)

(def action
  (pr-str [[:add-path [{:latitude 43.2 :longitude -70.0 :speed 1.4}
                       {:latitude 43.3 :longitude -70.0 :speed 1.5}]]]))

(defn post [body]
  (let [url (URL. "https://fierce-dawn-3931.herokuapp.com/api")
        connection (.openConnection url)]
    (doto connection
      (.setDoOutput true)
      (.setRequestProperty "Content-Type" "application/edn"))
    (try
      (let [output-stream (BufferedOutputStream. (.getOutputStream connection))]
        (try
          (do (doto output-stream
                (.write (.getBytes body))
                (.flush))
              (.getResponseCode connection))
          (finally
            (.close output-stream))))
      (catch Exception ex ex)
      (finally
        (.disconnect connection)))))

(def loading-ui
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text "Uploading..."}]
   [:progress-bar {}]])

(def success-ui
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text "Upload succeded"}]])

(defn failure-ui [activity msg]
  [:linear-layout
   {:orientation :vertical}
   [:text-view {:text msg}]
   [:button {:text "Retry"
             :on-click (fn [_] (upload-path activity))}]])

(defn render-ui [activity status]
  (threading/on-ui
    (activity/set-content-view!
      activity
      (condp = status
        :success success-ui
        :failure (failure-ui activity "Upload Failed")
        :loading loading-ui
        :disconnected (failure-ui activity "Network Disconnected")))))

(defn network-available? [activity]
  (let [connectivity (.getSystemService activity Context/CONNECTIVITY_SERVICE)
        network-info (.getActiveNetworkInfo connectivity)]
    (and network-info (.isConnected network-info))))

(defn upload-path [activity]
  (if (network-available? activity)
    (do
      (render-ui activity :loading)
      (future
        (let [result (post action)]
          (threading/on-ui
            (if (= result 200)
              (render-ui activity :success)
              (render-ui activity :failure))))))
      (render-ui activity :disconnected)))

(activity/defactivity
  android.sebluy.gpstracker.RemoteActivity
  :key :main
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (upload-path this)))


