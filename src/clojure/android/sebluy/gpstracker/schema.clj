(ns android.sebluy.gpstracker.schema
  (:require [gps-tracker-common.schema :as ct]
            [schema.core :as s]
            [schema.experimental.abstract-map :as abstract-map]))

;;;; Pages

(s/defschema Page
  (abstract-map/abstract-map-schema :id {}))

(s/defschema Status (s/enum :pending :success :failure :disconnected))

(abstract-map/extend-schema
 RemotePage Page [:remote] {:request s/Any
                            :status Status
                            (s/optional-key :future) s/Any})

(abstract-map/extend-schema
 PathListPage Page [:waypoint-path-list] {})

(abstract-map/extend-schema
 ShowPathPage Page [:show-waypoint-path] {:path-id ct/Date})

(abstract-map/extend-schema
 BluetoothPage Page [:bluetooth] {:status s/Keyword
                                  :request s/Any
                                  :adapter s/Any
                                  :devices s/Any
                                  (s/optional-key :write-queue) s/Any
                                  (s/optional-key :device) s/Any
                                  (s/optional-key :loader) s/Any
                                  (s/optional-key :scanner) s/Any})

;;;; Top Level State

; Todo: add list and activity validator

(s/defschema State (s/if empty?
                     {}
                     {:page Page
                      :history s/Any
                      :activity s/Any
                      (s/optional-key :waypoint-paths) [ct/WaypointPath]}))

(def validator (s/validator State))
