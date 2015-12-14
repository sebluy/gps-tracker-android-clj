(defproject gps-tracker/gps-tracker "0.0.1-SNAPSHOT"
  :description "GPS Tracker Android Application"
  :url "https://github.com/sebluy/gps-tracker-android-clj"

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :plugins [[lein-droid "0.4.1"]]

  ; neko 4.0.0-alpha4 wont compile

  :dependencies [[org.clojure-android/clojure "1.7.0-RC1" :use-resources true]
                 [neko/neko "4.0.0-alpha3"]
                 [com.google.android.gms/play-services-location "8.1.0" :extension "aar"]
                 [sebluy/gps-tracker-common "0.1.0"]
                 [prismatic/schema "1.0.1"]]

  :profiles {:default [:dev]

             :dev     [:android-common :android-user
                       {:dependencies [[org.clojure/tools.nrepl "0.2.10"]]
                        :target-path  "target/debug"
                        :android      {:aot                     :all-with-unused
                                       :rename-manifest-package "android.sebluy.gpstracker.debug"
                                       :manifest-options        {:app-name "GPSTracker - debug"}}}]
             :release [:android-common
                       {:target-path "target/release"
                        :android     {:ignore-log-priority [:debug :verbose]
                                      :aot                 :all
                                      :build-type          :release}}]}

  :android {
            :dex-opts       ["-JXmx2G" "--incremental"]
            :aot-exclude-ns [#"schema.*"
                             "clojure.parallel"
                             "clojure.core.reducers"]})
