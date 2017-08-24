(ns reagent-forms.page
  (:require
    [reagent.core :as r]
    [reagent-forms.core :as forms]))

(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(def form-template
  [:div
   (row "first name" [:input {:field :text :id :first-name}])
   (row "last name" [:input {:field :text :id :last-name}])
   (row "age" [:input {:field :numeric :id :age}])
   (row "email" [:input {:field :email :id :email}])
   (row "comments" [:textarea {:field :textarea :id :comments}])
   (row "date " [:div {:field :datepicker :id :birthday :date-format "yyyy/mm/dd"}])])

(defn home-page []
  (r/with-let [doc (r/atom {})]
    [:div
     [:div.page-header [:h1 "Reagent Form"]]
     [forms/bind-fields form-template doc]
     [:label (str @doc)]]))

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))


(defn init! []
  (mount-root))
