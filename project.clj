(defproject battle-arena "0.1.0-SNAPSHOT"
  :description "Multiplayer online battle-arena game (DotA2-like)."
  :url "https://github.com/mpereira/battle-arena"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [com.cemerick/piggieback "0.1.3"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :injections  [(require '[cljs.repl.browser :as browser]
                         '[cemerick.piggieback :as piggieback])
                (defn browser-repl []
                  (piggieback/cljs-repl :repl-env (browser/repl-env :port 9000)))]

  :cljsbuild {:repl-listen-port 9000
              :builds [{:id "battle-arena"
                        :source-paths ["src"]
                        :compiler {:output-to "battle_arena.js"
                                   :output-dir "out"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}]})
