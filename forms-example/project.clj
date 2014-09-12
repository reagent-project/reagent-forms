(defproject forms-example "0.1.0-SNAPSHOT"
  :description "A sample application using reagent-forms"
  :url "https://github.com/yogthos/reagent-forms"

  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [reagent-forms "0.1.7"]
   [json-html "0.2.2"]
   [org.clojure/clojurescript "0.0-2322"]
   [selmer "0.7.0"]
   [ring-server "0.3.1"]
   [lib-noir "0.8.8"]
   [environ "1.0.0"]]

  :ring {:handler forms-example.handler/app}

  :cljsbuild
  {:builds
   [{:source-paths ["src-cljs"],
     :id "dev",
     :compiler
     {:output-dir "resources/public/js/",
      :optimizations :none,
      :output-to "resources/public/js/app.js",
      :source-map true,
      :pretty-print true}}
    {:source-paths ["src-cljs"],
     :id "release",
     :compiler
     {:closure-warnings {:non-standard-jsdoc :off},
      :optimizations :advanced,
      :output-to "resources/public/js/app.js",
      :output-wrapper false,
      :pretty-print false}}]}

  :plugins
  [[lein-ring "0.8.10"]
   [environ "1.0.0"]
   [lein-cljsbuild "1.0.3"]]

  :jvm-opts ["-server"]

  :profiles
  {:uberjar {:aot :all},
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}},
   :dev
   {:dependencies
    [[ring-mock "0.1.5"]
     [ring/ring-devel "1.3.1"]
     [pjstadig/humane-test-output "0.6.0"]],
    :injections
    [(require 'pjstadig.humane-test-output)
     (pjstadig.humane-test-output/activate!)],
    :env {:dev true}}})
