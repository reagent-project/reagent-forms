(defproject reagent-forms "0.5.32"
  :description "data binding library for Reagent"
  :url "https://github.com/yogthos/reagent-forms"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :clojurescript? true
  :dependencies [[reagent "0.7.0"]]
  :plugins [[codox "0.6.4"]]
  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "1.9.908"]
                             [json-html "0.4.4"]
                             [com.cemerick/piggieback "0.2.2"]
                             [figwheel-sidecar "0.5.13"]]
              :plugins      [[lein-cljsbuild "1.0.3"]
                             [lein-figwheel "0.5.13"]]
              :source-paths ["src" "dev"]
              :resource-paths ["dev"]
              :figwheel
              {:http-server-root "public"
               :nrepl-port 7002
               :css-dirs ["resources"]
               :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
              :clean-targets ^{:protect false}
                            [:target-path
                             [:cljsbuild :builds :app :compiler :output-dir]
                             [:cljsbuild :builds :app :compiler :output-to]]
              :cljsbuild
                            {:builds
                             {:app
                              {:source-paths ["src" "dev"]
                               :figwheel     {:on-jsload "reagent-forms.page/mount-root"}
                               :compiler
                                             {:main          "reagent-forms.app"
                                              :asset-path    "js/out"
                                              :output-to     "dev/public/js/app.js"
                                              :output-dir    "dev/public/js/out"
                                              :source-map    true
                                              :optimizations :none
                                              :pretty-print  true}}}}}})
