# reagent-forms

A Clojure library designed to ... well, that part is up to you.

## Usage

This library provides a way to easily create data bindings for form components via the Reagent atom.

Each form components that we wish to bind must have a `:field` key. This key will be used to decide how the specific type of components should be bound. The field must also provie a unique `:id` key that will be used to bind it to the document. A simple text input field might look as follows:

```clojure
[:input {:field :text :id :first-name}]
```

The components behave just like any other Reagent components and can be mixed with them freely:

```clojure
(defn row [label input]
  [:div.row
    [:div.col-md-2 [:label label]]
    [:div.col-md-5 input]])
    
(def form-template
  [:div   
   [row "first name" [:input {:field :text :id :first-name}]]
   [:div.alert.alert-danger {:field :alert :id :first-name :event not-empty} "first name is populated"]
   [row "last name" [:input {:field :text :id :last-name}]]
   [:div.alert.alert-success {:field :alert :id :last-name :event not-empty} "last name is populated"]
   [row "date of birth" [:input {:field :numeric :id :dob}]]
   [row "email" [:input {:field :email :id :email}]]
   [row "comments" [:textarea {:field :textarea :id :comments}]]
   
   [:hr]
   [:h3 "BMI Calculator"]
   [row "Height" [:input {:field :numeric :id :height}]]
   [row "Weight" [:input {:field :numeric :id :weight}]]
   [row "BMI" [:input {:field :numeric :id :bmi :disabled true}]]
   [:hr]
   
   [row "are you happy?" [:input {:field :checkbox :id :happy}]]
   
   [:h3 "option list"]
   [:div.form-group
    [:label "pick an option"]
    [:select.form-control {:field :list :id :many.options}
     [:option {:key :foo} "foo"]
     [:option {:key :bar} "bar"]
     [:option {:key :baz} "baz"]]]
   [:select.form-control {:field :single-select :id :simple-select :multiple true}
    [:option {:key :one} 1]
    [:option {:key :two} 2]
    [:option {:key :three} 3]]  
     
   [:input {:field :radio :value :a :name :foo :id :radioselection} "foo"]
   [:input {:field :radio :value :b :name :foo :id :radioselection} "bar"]
   [:input {:field :radio :value :c :name :foo :id :radioselection} "baz"]
   
   [:h3 "multi-select buttons"]
   [:div.btn-group {:field :multi-select :id :position}
    [:button.btn.btn-default {:key :left} "Left"]
    [:button.btn.btn-default {:key :middle} "Middle"]
    [:button.btn.btn-default {:key :right} "Right"]]
   
   [:h3 "single-select buttons"]
   [:div.btn-group {:field :single-select :id :unique.position}
    [:button.btn.btn-default {:key :left} "Left"]
    [:button.btn.btn-default {:key :middle} "Middle"]
    [:button.btn.btn-default {:key :right} "Right"]]
   
   [:h3 "single-select list"]
   [:ul.list-group {:field :single-select :id :pick.one}
    [:li.list-group-item {:key :foo} "foo"]
    [:li.list-group-item {:key :bar} "bar"]
    [:li.list-group-item {:key :baz} "baz"]]
   
   [:h3 "multi-select list"]
   [:ul.list-group {:field :multi-select :id :pick.a.few}
    [:li.list-group-item {:key :foo} "foo"]
    [:li.list-group-item {:key :bar} "bar"]
    [:li.list-group-item {:key :baz} "baz"]]])
```
Once a form template is created it can be bound to a document using the `bind-fields` function:

```clojure
(ns myform.core
  (:require [reagent-forms.fields :as w])

(defn form []
  (let [doc (atom {})]
    (fn []
      [:div       
       [:div.page-header [:h1 "Reagent Form"]]
       [w/bind-fields form-template nil doc]
       [:label (str @doc)]])))
       
(reagent/render-component [form] (.getElementById js/document "container"))
```

## Adding events

Zero or more events can be passed in to the `bind-fields` function. Events are triggered whenever the document is updated. The events are executed in order they are listed, and each following event sees that document modified by its predecessor. The event must take 3 parameters, which are `id`, `value`, and `document`. The event can either return an updated document or `nil`, when `nil` is returned then the state of the document is unmodified.


The following is an example of an event to calculate the value of the `:bmi` key when the `:weight` and `:height` keys are populated:

```clojure
[w/bind-fields
  form-template
  doc
  (fn [id value {:keys [weight height] :as doc}]
    (when (and (some #{id} [:height :weight]) weight height)
      (assoc-in doc [:bmi] (/ weight (* height height)))))]
```

## Adding custom fields

Custom fields can be added by implementing the `reagent-forms.fields/init-field` multimethod. The method must
take two parameters, where the first parameter is the field component and the second is the options.

By default the options will contain the `get` and the `save!` keys. The `get` key points to a function that accepts an id and returns the value associated with it. The `save!` function accepts an id and a value that will be associated with it.

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
