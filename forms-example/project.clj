(defproject forms-example "0.1.0-SNAPSHOT"
  :description "A sample application using reagent-forms"
  :url "https://github.com/yogthos/reagent-forms"

  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [reagent-forms "0.3.0"]
   [json-html "0.2.3"]
   [org.clojure/clojurescript "0.0-2322"]
   [selmer "0.7.2"]
   [ring-server "0.3.1"]
   [lib-noir "0.9.4"]]

  :ring {:handler forms-example.handler/app}

  :cljsbuild
  {:builds
   [{:source-paths ["src-cljs"],
     :compiler
     {:output-dir "resources/public/js/",
      :optimizations :none,
      :output-to "resources/public/js/app.js",
      :source-map true,
      :pretty-print true}}]}

  :plugins
  [[lein-ring "0.8.10"]
   [lein-cljsbuild "1.0.3"]]

  :jvm-opts ["-server"]

  :profiles
  {:uberjar {:aot :all}
   :release {:ring {:open-browser? false
                    :stacktraces?  false
                    :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.3.0"]]}})
