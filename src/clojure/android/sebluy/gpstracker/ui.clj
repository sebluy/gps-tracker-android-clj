(ns android.sebluy.gpstracker.ui
  (:require [neko.ui.mapping :as mapping])
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

(defn table-row [title value]
  [:table-row {}
   [:text-view {:text (str title)}]
   [:text-view {:text (str value)}]])

(defn table [attributes]
  (into [:table-layout {}]
        (map (partial apply table-row)
             attributes)))

