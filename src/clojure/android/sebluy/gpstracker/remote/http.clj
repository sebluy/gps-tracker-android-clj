(ns android.sebluy.gpstracker.remote.http
  (:import [java.net URL]
           [java.io BufferedOutputStream BufferedReader InputStreamReader]
           [java.net HttpURLConnection]))

(def api-endpoint "https://fierce-dawn-3931.herokuapp.com/api")

(defn- write-connection [connection ^String body]
  (try
    (let [output-stream (BufferedOutputStream. (.getOutputStream connection))]
      (try
        (doto output-stream
          (.write (.getBytes body))
          (.flush))
        (finally
          (.close output-stream))))))

(defn- read-response [reader]
  (let [line-sequence (repeatedly #(.readLine reader))]
    (reduce str "" (take-while some? line-sequence))))

(defn- read-connection [connection]
  (let [input-reader (BufferedReader. (InputStreamReader. (.getInputStream connection)))]
    (try
      (let [response-code (.getResponseCode connection)
            response (read-response input-reader)]
        [response-code response])
      (finally
        (.close input-reader)))))

(defn post [body]
  (let [url (URL. api-endpoint)
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

