(defproject om-snake "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [om "0.6.1"]
                 [sablono "0.2.16"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "om-snake"
              :source-paths ["src"]
              :compiler {
                :output-to "om_snake.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
