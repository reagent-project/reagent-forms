# reagent-forms

A ClojureScript library to provide form data bindings for [Reagent](http://holmsand.github.io/reagent/), see [here](http://yogthos.github.io/reagent-forms-example.html) for a live demo.

## Install

[![Clojars Project](http://clojars.org/reagent-forms/latest-version.svg)](http://clojars.org/reagent-forms)

## Usage

The library uses a Reagent atom as the document store. The components are bound to the document using the `:field` attribute. This key will be used to decide how the specific type of component should be bound. The component must also provie a unique `:id` attribute that is used to correlated it to the document. While the library is geared towards usage with Twitter Bootstrap, it is fairly agnoistic about the types of components that you create.

The following types of fields are supported out of the box:

#### :input

An input fields can be of type `:text`, `:numeric`, `:password`, `:email`, and `:textarea`. The inputs behave just like regular HTML inputs and update the document state when the `:on-change` event is triggered.

```clojure
[:input {:field :text :id :first-name}]
[:input {:field :numeric :id :age}]
```

#### :checkbox

The checkbox field creates a checkbox element:

```clojure
[:div.row
  [:div.col-md-2 "does data binding make you happy?"]
  [:div.col-md-5 [:input {:field :checkbox :id :happy.bindings}]]]
```

#### :radio

Radio buttons are grouped using the `:name` attribute and their `:value` attribute is saved to the document:

```clojure
[:input {:field :radio :value :a :name :foo :id :radioselection} "foo"]
[:input {:field :radio :value :b :name :foo :id :radioselection} "bar"]
[:input {:field :radio :value :c :name :foo :id :radioselection} "baz"]
```
#### :list

The list field will populate the document with the currently selected child element. The child elements must each have a `:key` attribute pointing to the value that will be saved in the document:

```clojure
[:select.form-control {:field :list :id :many.options}
  [:option {:key :foo} "foo"]
  [:option {:key :bar} "bar"]
  [:option {:key :baz} "baz"]]
```

#### :single-select

The single-select field behaves like the list, but supports different types of elements and allows the fields to be deselected. The `:key` attribute of the selected child element is persisted:

```clojure
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
```

#### :multi-select

The multi-select field allows multiple values to be selected and set in the document, the `:key` attribute of the selected children are persisted:

```clojure
[:h3 "multi-select list"]
[:div.btn-group {:field :multi-select :id :position}
  [:button.btn.btn-default {:key :left} "Left"]
  [:button.btn.btn-default {:key :middle} "Middle"]
  [:button.btn.btn-default {:key :right} "Right"]]
```

#### :alert

Alerts are bound to an id of a field that triggers the alert and can have an optional `:event` key. The event key should point to a function that returns a boolean value.

When an event is supplied then the body of the alert is rendered wheneve the event returns true:

```clojure
[:input {:field :text :id :first-name}]
[:div.alert.alert-success {:field :alert :id :last-name :event empty?} "first name is empty!"]
```

When no event is supplied, then the alert is shown whenever the value at the id is not empty and displays the value:

```clojure
(def doc (atom {}))

;;define an alert that watches the `:errors.first-name` key for errors
[:div.alert.alert-danger {:field :alert :id :errors.first-name}]

;;trigger the alert by setting the error key
[:button.btn.btn-default
  {:on-click
    #(if (empty? (:first-name @doc))
      (swap! doc assoc :errors.first-name "first name is empty!"))}
  "save"]
```

The field components behave just like any other Reagent components and can be mixed with them freely. A complete form example can be seen below.

```clojure
(defn row [label input]
  [:div.row
    [:div.col-md-2 [:label label]]
    [:div.col-md-5 input]])

(def form-template
  [:div
   [row "first name" [:input {:field :text :id :first-name}]]
   [row "last name" [:input {:field :text :id :last-name}]]
   [row "age" [:input {:field :numeric :id :age}]]
   [row "email" [:input {:field :email :id :email}]]
   [row "comments" [:textarea {:field :textarea :id :comments}]]])
```
Once a form template is created it can be bound to a document using the `bind-fields` function:

```clojure
(ns myform.core
  (:require [reagent-forms.core :refer [bind-fields]])

(defn form []
  (let [doc (atom {})]
    (fn []
      [:div
       [:div.page-header [:h1 "Reagent Form"]]
       [bind-fields form-template nil doc]
       [:label (str @doc)]])))

(reagent/render-component [form] (.getElementById js/document "container"))
```

The form can be initialized with a populated document, and the fields will be initialize with the values found there:

```clojure
(def form-template
  [:div
   [row "first name" [:input {:field :text :id :first-name}]]
   [row "last name" [:input {:field :text :id :last-name}]]
   [row "age" [:input {:field :numeric :id :age}]]
   [row "email" [:input {:field :email :id :email}]]
   [row "comments" [:textarea {:field :textarea :id :comments}]]])

(defn form []
  (let [doc (atom {:first-name "John" :last-name "Doe" :age 35})]
    (fn []
      [:div
       [:div.page-header [:h1 "Reagent Form"]]
       [bind-fields form-template nil doc]
       [:label (str @doc)]])))
```

## Adding events

The `bind-fields` function accepts optional events. Events are triggered whenever the document is updated, and will be executed in order they are listed. Each event sees the document modified by its predecessor.

The event must take 3 parameters, which are the `id`, the `value`, and the `document`. The `id` and the `value` represent the value that was changed in the form, and the document is the atom that contains the state of the form. The event can either return an updated document or `nil`, when `nil` is returned then the state of the document is unmodified.

The following is an example of an event to calculate the value of the `:bmi` key when the `:weight` and `:height` keys are populated:

```clojure
(defn row [label input]
  [:div.row
    [:div.col-md-2 [:label label]]
    [:div.col-md-5 input]])

(def form-template
 [:div
   [:h3 "BMI Calculator"]
   [row "Height" [:input {:field :numeric :id :height}]]
   [row "Weight" [:input {:field :numeric :id :weight}]]
   [row "BMI" [:input {:field :numeric :id :bmi :disabled true}]]])

[w/bind-fields
  form-template
  doc
  (fn [id value {:keys [weight height] :as doc}]
    (when (and (some #{id} [:height :weight]) weight height)
      (assoc-in doc [:bmi] (/ weight (* height height)))))]
```

## Adding custom fields

Custom fields can be added by implementing the `reagent-forms.core/init-field` multimethod. The method must
take two parameters, where the first parameter is the field component and the second is the options.

By default the options will contain the `get` and the `save!` keys. The `get` key points to a function that accepts an id and returns the document value associated with it. The `save!` function accepts an id and a value that will be associated with it.

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
