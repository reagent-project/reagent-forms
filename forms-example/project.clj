(defproject forms-example "0.1.0-SNAPSHOT"
  :description "A sample application using reagent-forms"
  :url "https://github.com/yogthos/reagent-forms"

  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [reagent-forms "0.5.8"]
   [json-html "0.3.5"]
   [org.clojure/clojurescript "1.7.122"]
   [selmer "0.9.1"]
   [ring-server "0.4.0"]
   [lib-noir "0.9.7"]]

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
   [lein-cljsbuild "1.0.6"]]

  :jvm-opts ["-server"]

  :profiles
  {:uberjar {:aot :all}
   :release {:ring {:open-browser? false
                    :stacktraces?  false
                    :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.4.0"]]}})
