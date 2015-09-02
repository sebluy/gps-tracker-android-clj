(ns android.sebluy.gpstracker.gps
  (:require [clojure.core.async :as async]
            [android.sebluy.gpstracker.state :as state])
  (:import [com.google.android.gms.common.api GoogleApiClient$Builder
                                              GoogleApiClient$ConnectionCallbacks
                                              GoogleApiClient$OnConnectionFailedListener]
           [com.google.android.gms.location LocationServices
                                            LocationRequest
                                            LocationCallback]
           [android.os Looper]))

(defn make-connection-callbacks [chan]
  (reify GoogleApiClient$ConnectionCallbacks
    (onConnected [_ _]
      (async/put! chan :connected))
    (onConnectionSuspended [_ _]
      (async/put! chan :disconnected))))

(defn make-on-connection-failed-listener [chan]
  (reify GoogleApiClient$OnConnectionFailedListener
    (onConnectionFailed [_ _]
      (async/put! chan :disconnected))))

(defn make-google-api-client [activity chan]
  (.. (GoogleApiClient$Builder. activity)
      (addConnectionCallbacks (make-connection-callbacks chan))
      (addOnConnectionFailedListener (make-on-connection-failed-listener chan))
      (addApi LocationServices/API)
      (build)))

(defn make-location-callback [control-chan location-chan]
  (proxy [LocationCallback] []
    (onLocationAvailability [availability]
      (if (.isLocationAvailable availability)
        (async/put! control-chan :tracking-started)
        (async/put! control-chan :tracking-stopped)))
    (onLocationResult [location-result]
      (async/put! location-chan (.getLastLocation location-result)))))

(defn make-location-request []
  (.. (LocationRequest.)
      (setInterval 1000)
      (setPriority LocationRequest/PRIORITY_HIGH_ACCURACY)))

(defn start-location-updates [google-api-client location-listener]
  (.requestLocationUpdates LocationServices/FusedLocationApi
                           google-api-client
                           (make-location-request)
                           location-listener
                           (Looper/getMainLooper)))

(defn stop-location-updates [google-api-client location-listener]
  (.removeLocationUpdates LocationServices/FusedLocationApi
                          google-api-client
                          location-listener))

(defn run-location-machine [activity]
  (let [status-chan (async/chan)
        control-chan (async/chan)
        location-chan (async/chan)
        location-callback (make-location-callback control-chan location-chan)
        google-api-client (make-google-api-client activity control-chan)]
    (async/go-loop [state :disconnected]
      (async/put! status-chan state)
      (condp = [state (async/<! control-chan)]
        [:disconnected :start]
        (do (.connect google-api-client)
            (recur :connecting))
        [:connecting :connected]
        (do (start-location-updates google-api-client location-callback)
            (recur :connected))
        [:connecting :disconnected]
        (recur :disconnected)
        [:connected :tracking-started]
        (recur :tracking)
        [:connected :stop]
        (do (.disconnect google-api-client)
            (recur :disconnected))
        [:connected :disconnected]
        (recur :disconnected)
        [:tracking :stop]
        (do (stop-location-updates google-api-client location-callback)
            (.disconnect google-api-client)
            (recur :disconnected))
        (recur state)))
    [status-chan control-chan location-chan]))

