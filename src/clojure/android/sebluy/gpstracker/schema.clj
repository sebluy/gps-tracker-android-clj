(ns android.sebluy.gpstracker.schema
  (:require [schema.core :as s]
            [schema.experimental.abstract-map :as abstract-map]))

(s/defschema PathType (s/enum :tracking :waypoint))

;;;; Points

(s/defschema Point
  (abstract-map/abstract-map-schema :type {:latitude s/Num :longitude s/Num}))

(abstract-map/extend-schema
 Waypoint Point [:waypoint] {})

(abstract-map/extend-schema
 TrackingPoint Point [:tracking]
 {(s/optional-key :speed) s/Num (s/optional-key :accuracy) s/Num})

;;;; Paths

(s/defschema Path
  (abstract-map/abstract-map-schema :type {:id s/Int}))

(abstract-map/extend-schema
 TrackingPath Path [:tracking] {:points [TrackingPoint]})

(abstract-map/extend-schema
 WaypointPath Path [:waypoint] {:points [Waypoint]})

;;;; Pages

(s/defschema Page
  (abstract-map/abstract-map-schema :id {}))

(abstract-map/extend-schema
 MainPage Page [:main] {})

(s/defschema Status (s/enum :pending :success :failure :disconnected))
(s/defschema Request (s/enum :get-waypoint-paths))

(abstract-map/extend-schema
 RemotePage Page [:remote] {:request Request :status Status})

(abstract-map/extend-schema
 PathListPage Page [:path-list] {:path-type PathType})

(abstract-map/extend-schema
 ShowPathPage Page [:path] {:path-type PathType :path-id s/Int})

;;;; Top Level State

; Todo: add list and activity validator

(s/defschema State {:page Page
                    :history s/Any
                    :activity s/Any
                    (s/optional-key :waypoint-paths) [WaypointPath]})

(def validator (s/validator State))
