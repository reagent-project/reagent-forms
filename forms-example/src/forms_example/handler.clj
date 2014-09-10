(ns forms-example.handler
  (:require [compojure.core :refer [GET defroutes]]
            [noir.util.middleware :refer [app-handler]]
            [compojure.route :as route]
            [selmer.parser :as parser]
            [environ.core :refer [env]]))

(defn resource [r]
 (-> (Thread/currentThread)
     (.getContextClassLoader)
     (.getResource r)
     slurp))

(defroutes base-routes
  (GET "/" []
    (parser/render-file "templates/app.html"
                        {:dev (env :dev)
                         :json-css (resource "json.human.css")}))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app (app-handler [base-routes]))
