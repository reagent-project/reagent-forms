(ns forms-example.core
  (:require [json-html.core :refer [edn->hiccup]]
            [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :refer [bind-fields init-field value-of]]))

(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn radio [label id name value]
  [:div.radio
   [:label
    [:input {:field :radio :id id :name name :value value}]
    label]])

(defn input [label type id]
  (row label [:input.form-control {:field type :id id}]))

(def form-template
  [:div
   (input "first name" :text :first-name)
   [:div.row
    [:div.col-md-2]
    [:div.col-md-5
     [:div.alert.alert-danger {:field :alert :id :errors.first-name}]]]

   (input "last name" :text :last-name)
   [:div.row
    [:div.col-md-2]
    [:div.col-md-5
     [:div.alert.alert-success {:field :alert :id :last-name :event empty?}
      "last name is empty!"]]]

   (input "age" :numeric :age)
   (input "email" :email :email)
   (row
    "comments"
    [:textarea.form-control {:field :textarea :id :comments}])

   [:hr]
   [:h3 "BMI Calculator"]
   (input "height" :numeric :height)
   (input "weight" :numeric :weight)
   (row "BMI" [:input.form-control {:field :numeric :id :bmi :disabled true}])
   [:hr]

   [row "isn't data binding lovely?" [:input {:field :checkbox :id :databinding.lovely}]]
   [:label {:field :label :preamble "The level of awesome: " :placeholder "N/A" :id :awesomeness}]

   [:input {:field :range :min 1 :max 10 :id :awesomeness}]

   [:h3 "option list"]
   [:div.form-group
    [:label "pick an option"]
    [:select.form-control {:field :list :id :many.options}
     [:option {:key :foo} "foo"]
     [:option {:key :bar} "bar"]
     [:option {:key :baz} "baz"]]]

   (radio
    "Option one is this and that—be sure to include why it's great"
    :radioselection :foo :a)
   (radio
    "Option two can be something else and selecting it will deselect option one"
    :radioselection :foo :b)

   [:h3 "multi-select buttons"]
   [:div.btn-group {:field :multi-select :id :every.position}
    [:button.btn.btn-default {:key :left} "Left"]
    [:button.btn.btn-default {:key :middle} "Middle"]
    [:button.btn.btn-default {:key :right} "Right"]]

   [:h3 "single-select buttons"]
   [:div.btn-group {:field :single-select :id :unique.position}
    [:button.btn.btn-default {:key :left} "Left"]
    [:button.btn.btn-default {:key :middle} "Middle"]
    [:button.btn.btn-default {:key :right} "Right"]]

   [:h3 "single-select list"]
   [:div.list-group {:field :single-select :id :pick.one}
    [:div.list-group-item {:key :foo} "foo"]
    [:div.list-group-item {:key :bar} "bar"]
    [:div.list-group-item {:key :baz} "baz"]]

   [:h3 "multi-select list"]
   [:ul.list-group {:field :multi-select :id :pick.a.few}
    [:li.list-group-item {:key :foo} "foo"]
    [:li.list-group-item {:key :bar} "bar"]
    [:li.list-group-item {:key :baz} "baz"]]])

(defn page []
  (let [doc (atom {:first-name "John"
                    :dob 12345
                    :email "foo@bar.baz"
                    :comments "some interesting comments\non this subject"
                    :weight 100
                    :height 200
                    :bmi 0.5
                    :radioselection :b
                    :position [:left :right]
                    :pick.one :bar
                    :unique.position :middle
                    :pick.a.few [:bar :baz]
                    :many.options :bar})]
    (fn []
      [:div
       [:div.page-header [:h1 "Sample Form"]]
       [bind-fields
        form-template
        doc
        (fn [id value {:keys [weight height] :as document}]
          (when (and (some #{id} [:height :weight]) weight height)
            (assoc document :bmi (/ weight (* height height)))))]

       [:button.btn.btn-default
         {:on-click
          #(if (empty? (:first-name @doc))
             (swap! doc assoc :errors.first-name "first name is empty"))}
         "save"]

       [:hr]
       [:h1 "Document State"]
       [:p (edn->hiccup @doc)]])))

(reagent/render-component [page] (.getElementById js/document "app"))
