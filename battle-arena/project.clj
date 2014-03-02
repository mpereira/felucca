(defproject battle-arena "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 ;; [om "0.5.0"]
                 ]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {:builds [{:id "battle-arena"
                        :source-paths ["src"]
                        :compiler {:output-to "battle_arena.js"
                                   :output-dir "out"
                                   :optimizations :none
                                   :source-map true}}]})
