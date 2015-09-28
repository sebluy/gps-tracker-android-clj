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

(post (pr-str [[:get-waypoint-path-ids]]))

(defn write-connection [connection ^String body]
  (try
    (let [output-stream (BufferedOutputStream. (.getOutputStream connection))]
      (try
        (doto output-stream
          (.write (.getBytes body))
          (.flush))
        (finally
          (.close output-stream))))))

(defn read-response [reader]
  (let [line-sequence (repeatedly #(.readLine reader))]
    (reduce str "" (take-while some? line-sequence))))

(defn read-connection [connection]
  (let [input-reader (BufferedReader. (InputStreamReader. (.getInputStream connection)))]
    (try
      (let [response-code (.getResponseCode connection)
            response (read-response input-reader)]
        [response-code response])
      (finally
        (.close input-reader)))))

(defn post [body]
  (let [url (URL. "https://fierce-dawn-3931.herokuapp.com/api")
        ^HttpURLConnection connection (.openConnection url)]
    (doto connection
      (.setDoOutput true)
      (.setDoInput true)
      (.setRequestProperty "Content-Type" "application/edn"))
    (try
      (do (write-connection connection body)
          (read-connection connection))
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
             :on-click (fn [_] (state/handle upload-path activity))}]])

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

(defn update-ui [activity state]
  (render-ui activity (get-in state [:remote :status])))

(defn handle-response [activity [response-code _] state]
  (let [new-status (if (= response-code 200) :success :failure)
        new-state (assoc-in state [:remote :status] new-status)]
    (update-ui activity new-state)
    new-state))

(defn upload-path [activity state]
  (let [network? (network-available? activity)
        new-status (if network? :pending :disconnected)
        new-state (assoc-in state [:remote :status] new-status)]
    (when network?
      (let [path (get-in state [:remote :path])]
        (future
          (let [response-attrs (-> path path->action pr-str post)]
            (state/handle handle-response activity response-attrs)))))
    (update-ui activity new-state)
    new-state))

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
    (state/handle upload-path this))
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


