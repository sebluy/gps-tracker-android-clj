(defproject gps-tracker/gps-tracker "0.0.1-SNAPSHOT"
  :description "FIXME: Android project description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/clojure" "src"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :plugins [[lein-droid "0.4.1"]]

  ; neko 4.0.0-alpha4 wont compile
  ; problem with usb connection? crashes on disconnect/reconnect when repl is active

  :dependencies [[org.clojure-android/clojure "1.7.0-RC1" :use-resources true]
                 [neko/neko "4.0.0-alpha3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.google.android.gms/play-services-location "7.8.0" :extension "aar"]]
  ; comment out play-services-location and refresh in cursive to avoid maven error

  :profiles {:default [:dev]

             :dev     [:android-common :android-user
                       {:dependencies [[org.clojure/tools.nrepl "0.2.10"]]
                        :target-path  "target/debug"
                        :android      {:aot                     :all-with-unused
                                       :rename-manifest-package "android.sebluy.gpstracker.debug"
                                       :manifest-options        {:app-name "GPSTracker - debug"}}}]
             :release [:android-common
                       {:target-path "target/release"
                        :android     {;; Specify the path to your private keystore
                                      ;; and the the alias of the key you want to
                                      ;; sign APKs with.
                                      ;; :keystore-path "/home/user/.android/private.keystore"
                                      ;; :key-alias "mykeyalias"

                                      :ignore-log-priority [:debug :verbose]
                                      :aot                 :all
                                      :build-type          :release}}]}

  :android {;; Specify the path to the Android SDK directory.
            ;; :sdk-path "/home/user/path/to/android-sdk/"

            ;; Try increasing this value if dexer fails with
            ;; OutOfMemoryException. Set the value according to your
            ;; available RAM.
            :dex-opts       ["-JXmx4096M" "--incremental"]

            ;; If previous option didn't work, uncomment this as well.
            ;; :force-dex-optimize true

            :target-version "19"
            :aot-exclude-ns ["cljs.core.async.macros"
                             "cljs.core.async.impl.ioc-macros"
                             "clojure.parallel" "clojure.core.reducers"
                             "cljs-tooling.complete" "cljs-tooling.info"
                             "cljs-tooling.util.analysis" "cljs-tooling.util.misc"
                             "cider.nrepl" "cider-nrepl.plugin"
                             "cider.nrepl.middleware.util.java.parser"]})


