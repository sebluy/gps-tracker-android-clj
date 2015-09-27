(ns android.sebluy.gpstracker.remote
  (:require [android.sebluy.gpstracker.state :as state]
            [neko.activity :as activity]
            [neko.threading :as threading]
            [android.sebluy.gpstracker.util :as util])
  (:import [java.net URL]
           [java.io BufferedOutputStream BufferedReader InputStreamReader]
           [android.content Context]
           [android.net ConnectivityManager]
           [java.net HttpURLConnection]
           [android.app Activity]))

(declare upload-path)

(defn path->action [path]
  [[:add-path
    (->> (path :points)
         (map (fn [point]
                (select-keys point #{:latitude :longitude :speed :accuracy})))
         (into []))]])

(defn post [^String body]
  (let [url (URL. "https://fierce-dawn-3931.herokuapp.com/api")
        ^HttpURLConnection connection (.openConnection url)]
    (doto connection
      (.setDoOutput true)
      (.setDoInput true)
      (.setRequestProperty "Content-Type" "application/edn"))
    (try
      (let [output-stream (BufferedOutputStream. (.getOutputStream connection))]
        (try
          (do (doto output-stream
                (.write (.getBytes body))
                (.flush)))
          (finally
            (.close output-stream))))
      (let [input-reader (BufferedReader. (InputStreamReader. (.getInputStream connection)))]
        (try
          (let [response-code (.getResponseCode connection)
                response (loop [response ""]
                           (if-let [response-chunk (.readLine input-reader)]
                             (recur (str response response-chunk))
                             response))]
            [response-code response])
          (finally
            (.close input-reader))))
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
   [:button {:text     "Retry"
             :on-click (fn [_] (upload-path activity (@state/state :path)))}]])

(defn render-ui [activity status]
  (threading/on-ui
    (activity/set-content-view!
      activity
      (condp = status
        :success success-ui
        :failure (failure-ui activity "Upload Failed")
        :loading loading-ui
        :disconnected (failure-ui activity "Network Disconnected")))))

(defn network-available? [^Activity activity]
  (let [connectivity (.getSystemService activity Context/CONNECTIVITY_SERVICE)
        network-info (.getActiveNetworkInfo ^ConnectivityManager connectivity)]
    (and network-info (.isConnected network-info))))

(defn upload-path [activity path]
  (if (network-available? activity)
    (do
      (render-ui activity :loading)
      (future
        (let [result (-> path path->action pr-str post)]
          (threading/on-ui
            (if (= result 200)
              (render-ui activity :success)
              (render-ui activity :failure))))))
    (render-ui activity :disconnected)))

#_(defn get-waypoints [activity]
    (if (network-available? activity)
      (do
        (render-ui activity :loading)
        (future
          (let [result (-> path path->action pr-str post)]
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
    (upload-path this (@state/state :upload)))
  (onStart
    [this]
    (.superOnStart this)
    (util/keep-screen-on this true))
  (onStop
    [this]
    (.superOnStop this)
    (util/keep-screen-on this false))
  (onBackPressed
    [this]
    (.superOnBackPressed this)
    (swap! state/state dissoc :upload)))


