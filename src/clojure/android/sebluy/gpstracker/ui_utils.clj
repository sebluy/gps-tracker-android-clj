(ns android.sebluy.gpstracker.ui-utils
  (:require [neko.ui.mapping :as mapping]
            [clojure.string :as string])
  (:import [android.widget TableLayout
                           TableRow]))

(mapping/defelement
  :table-layout
  :classname TableLayout
  :inherits :view-group)

(mapping/defelement
  :table-row
  :classname TableRow
  :inherits :view)

(defn table-row [value]
  [:table-row {}
   [:text-view {:text value}]])

(defn keyword->title [keyword]
  (-> :total-distance
      (name)
      (string/split #"-")
      (->> (map string/capitalize)
           (string/join " "))))

(defn readable-attribute [[key value]]
  [(keyword->title key) (str value)])

(defn table [attributes]
  (into [:table-layout {}]
        (map table-row
             (mapcat readable-attribute attributes))))
