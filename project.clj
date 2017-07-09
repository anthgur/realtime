(defproject realtime "0.1.0-SNAPSHOT"
  :min-lein-version "2.7.1"
  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [org.clojure/core.async "0.3.443"]
   ;; juxt.modular/http-kit blows up without this
   ;; wait for next release
   ;; https://github.com/juxt/modular/commit/083f50c144966f69d2ae23327a95f628aadb73f1
   [org.clojure/tools.logging "0.4.0"]
   [bidi "2.1.1"]
   [com.google.transit/gtfs-realtime-bindings "0.0.4"]
   [com.stuartsierra/component "0.3.2"]
   [fogus/ring-edn "0.3.0"]
   [http-kit "2.2.0"]
   [juxt.modular/bidi "0.9.5"]
   [juxt.modular/http-kit "0.5.4"]
   [juxt.modular/ring "0.5.3"]
   [org.flatland/protobuf "0.8.1"]
   [ring/ring-defaults "0.3.0"]
   [com.taoensso/timbre "4.10.0"]

   [org.clojure/clojurescript "1.9.227"]
   [cljsjs/material-ui "0.18.3-0"]
   [reagent "0.7.0" :exclusions [cljsjs/react cljsjs/react-dom]]
   [re-frame "0.9.4"]
   [cljsjs/leaflet "1.0.3-1"]]

  :resource-paths ["resources"]
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]

  :plugins
  [[lein-cljsbuild "1.1.3"]
   [lein-ancient "0.6.10"]]
  
  :cljsbuild
  {:builds
   [{:id :main
     :source-paths ["src/cljs"]
     :compiler     {:output-to "resources/public/js/app.js"
                    :optimizations :simple}}]}

  :hooks [leiningen.cljsbuild]

  :prep-tasks
  ["javac" "compile"]

  :uberjar-name "realtime.jar"

  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[org.clojure/data.csv "0.1.4"]
                        [reloaded.repl "0.2.3"]]}
   :uberjar {:aot :all
             :main realtime.server}}

  :repl-options
  {:init-ns user})
