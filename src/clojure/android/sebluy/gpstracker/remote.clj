(ns android.sebluy.gpstracker.remote
  (:require [neko.activity :as activity]
            [neko.threading :as threading])
  (:import java.net.URL
           java.io.BufferedOutputStream
           java.net.HttpURLConnection))

(defn post [body]
  (let [url (URL. "https://fierce-dawn-3931.herokuapp.com/api")
        ^HttpURLConnection connection (.openConnection url)]
    (doto connection
      (.setChunkedStreamingMode (count body))
      (.setDoOutput true)
      (.setRequestProperty "Content-Type" "application/edn")
      (.setRequestProperty "charset" "utf-8")
      (.setRequestProperty "Content-Length" (str (count body))))
    (let [output-stream (BufferedOutputStream. (.getOutputStream connection))]
      (doto output-stream
        (.write (.getBytes body))
        (.flush))
      (.getResponseCode connection))))

(def action
  (pr-str [[:add-path [{:latitude 43.2 :longitude -70.0 :speed 1.4}
                       {:latitude 43.3 :longitude -70.0 :speed 1.5}]]]))
(post action)

(defn render-ui [activity]
  (threading/on-ui
    (activity/set-content-view!
      activity
      [:linear-layout
       {:orientation :vertical}
       [:progress-bar {}]
       [:text-view {:text "Status"}]])))

(activity/defactivity
  android.sebluy.gpstracker.RemoteActivity
  :key :remote
  (onCreate
    [this bundle]
    (.superOnCreate this bundle)
    (render-ui this)))

