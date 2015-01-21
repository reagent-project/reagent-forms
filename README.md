# reagent-forms

A ClojureScript library to provide form data bindings for [Reagent](http://holmsand.github.io/reagent/), see [here](http://yogthos.github.io/reagent-forms-example.html) for a live demo.

## Install

[![Clojars Project](http://clojars.org/reagent-forms/latest-version.svg)](http://clojars.org/reagent-forms)

## Usage

The library uses a Reagent atom as the document store. The components are bound to the document using the `:field` attribute. This key will be used to decide how the specific type of component should be bound. The component must also provide a unique `:id` attribute that is used to correlate it to the document. While the library is geared towards usage with Twitter Bootstrap, it is fairly agnostic about the types of components that you create.

The following types of fields are supported out of the box:

#### :input

An input field can be of type `:text`, `:numeric`, `:range`, `:password`, `:email`, and `:textarea`. The inputs behave just like regular HTML inputs and update the document state when the `:on-change` event is triggered.

```clojure
[:input.form-control {:field :text :id :first-name}]
[:input.form-control {:field :numeric :id :age}]
```

The input fields can have an optional `:fmt` attribute that can provide a format string for the value:

```clojure
[:input.form-control
  {:field :numeric :fmt "%.2f" :id :bmi :disabled true}]
```
#### :typeahead

The typeahead field uses a `:data-source` key bound to a function that takes the current input and returns a list of matching results:

```clojure
(defn friend-source [text]
  (filter
    #(-> % (.toLowerCase %) (.indexOf text) (> -1))
    ["Alice" "Alan" "Bob" "Beth" "Jim" "Jane" "Kim" "Rob" "Zoe"]))

[:div {:field :typeahead :id :ta :data-source friend-source}]
```

#### :checkbox

The checkbox field creates a checkbox element:

```clojure
[:div.row
  [:div.col-md-2 "does data binding make you happy?"]
  [:div.col-md-5
   [:input.form-control {:field :checkbox :id :happy-bindings}]]]
```
#### :range

Range control uses the `:min` and `:max` keys to create an HTML range input:

```clojure
[:input.form-control
 {:field :range :min 10 :max 100 :id :some-range}]
```

#### :radio

Radio buttons are grouped using the `:name` attribute and their `:value` attribute is saved to the document:

```clojure
[:input {:field :radio :value :a :name :foo :id :radioselection} "foo"]
[:input {:field :radio :value :b :name :foo :id :radioselection} "bar"]
[:input {:field :radio :value :c :name :foo :id :radioselection} "baz"]
```

### Lists

List fields contain child elements whose values are populated in the document when they are selected. The child elements must each have a `:key` attribute pointing to the value that will be saved in the document. The value of the element must be a keyword.

The elements can have an optional `:visible?` keyword that points to a predicate function. The function should accept the document and return a boolean value indicatiing whether the field should be shown.

#### :list

The `:list` field is used for creating HTML `select` elements containing `option` child elements:

```clojure
[:select.form-control {:field :list :id :many-options}
  [:option {:key :foo} "foo"]
  [:option {:key :bar} "bar"]
  [:option {:key :baz} "baz"]]

(def months
  ["January" "February" "March" "April" "May" "June"
   "July" "August" "September" "October" "November" "December"])

[:select {:field :list :id :dob.day}
      (for [i (range 1 32)]
        [:option
         {:key (keyword (str i))
          :visible? #(let [month (get-in % [:dob :month])]
                       (cond
                        (< i 29) true
                        (< i 31) (not= month :February)
                        (= i 31) (some #{month} [:January :March :May :July :July :October :December])
                        :else false))}
          i])]
[:select {:field :list :id :dob.month}
  (for [month months]
    [:option {:key (keyword month)} month])]
[:select {:field :list :id :dob.year}
  (for [i (range 1950 (inc (.getFullYear (js/Date.))))]
    [:option {:key (keyword (str i))} i])]
```


#### :single-select

The single-select field behaves like the list, but supports different types of elements and allows the fields to be deselected:

```clojure
[:h3 "single-select buttons"]
[:div.btn-group {:field :single-select :id :unique-position}
  [:button.btn.btn-default {:key :left} "Left"]
  [:button.btn.btn-default {:key :middle} "Middle"]
  [:button.btn.btn-default {:key :right} "Right"]]

[:h3 "single-select list"]
[:ul.list-group {:field :single-select :id :pick-one}
  [:li.list-group-item {:key :foo} "foo"]
  [:li.list-group-item {:key :bar} "bar"]
  [:li.list-group-item {:key :baz} "baz"]]
```

#### :multi-select

The multi-select field allows multiple values to be selected and set in the document:

```clojure
[:h3 "multi-select list"]
[:div.btn-group {:field :multi-select :id :position}
  [:button.btn.btn-default {:key :left} "Left"]
  [:button.btn.btn-default {:key :middle} "Middle"]
  [:button.btn.btn-default {:key :right} "Right"]]
```

#### :label

Labels can be associated with a key in the document using the `:id` attribute and will display the value at that key. The lables can have an optional `:preamble` and `:postamble` keys with the text that will be rendered before and after the value respectively. The `:placeholder` key can be used to provide text that will be displayed in absence of a value:

```clojure
[:label {:field :label :id :volume}]
[:label {:field :label :preamble "the value is: " :id :volume}]
[:label {:field :label :preamble "the value is: " :postamble "ml" :id :volume}]
[:label {:field :label :preamble "the value is: " :postamble "ml" :placeholder "N/A" :id :volume}]

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
      (swap! doc assoc-in [:errors :first-name] "first name is empty!"))}
  "save"]
```

#### :datepicker

```clojure
[:div {:field :datepicker :id :birthday :date-format "yyyy/mm/dd" :inline true}]
```
The date is stored in the document using the following format:

```clojure
{:year 2014 :month 11 :day 24}
```

The date picker can also take an optional `:auto-close?` key to indicate that it should be closed when the day is clicked. This defaults to `false`.


The datepicker requires additional CSS in order to be rendered correctly. The default CSS is provided
in `reagent-forms.css` in the resource path. Simply make sure that it's included on the page.
The File can be read using:

```clojure
(-> "reagent-forms.css" clojure.java.io/resource slurp)
```

#### :container

The container element can be used to group different element.
The container can be used to set the visibility of multiple elements.

```clojure
[:div.form-group
 {:field :container
  :visible? #(:show-name? %)}
 [:input {:field :text :id :first-name}]
 [:input {:field :text :id :last-name}]]
```

### Setting component visibility

The components may  supply an optional `:visible?` key in their attributes that points to a decision function.
The function is expected to take the current value of the document and produce a truthy value that will be used
to decide whether the component should be rendered, eg:

```clojure
(def form
  [:div
   [:input {:field :text
            :id :foo}]
   [:input {:field :text
            :visible? #(empty? (:foo %))
            :id :bar}]])
```

## Binding the form to a document

The field components behave just like any other Reagent components and can be mixed with them freely. A complete form example can be seen below.

Form elements can be bound to a nested structure by using the `.` as a path separator. For example, the following component `[:input {:field :text :id :person.first-name}]` binds to the following path in the state atom `{:person {:first-name <field-value>}}`


```clojure
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
   (row "comments" [:textarea {:field :textarea :id :comments}])])
```

**important note**

The templates are eagerly evaluated, and you should always call the helper functions as in the example above instead of putting them in a vector. These will be replaced by Reagent components when the `bind-field` is called to compile the template.

Once a form template is created it can be bound to a document using the `bind-fields` function:

```clojure
(ns myform.core
  (:require [reagent-forms.core :refer [bind-fields]]))

(defn form []
  (let [doc (atom {})]
    (fn []
      [:div
       [:div.page-header [:h1 "Reagent Form"]]
       [bind-fields form-template doc]
       [:label (str @doc)]])))

(reagent/render-component [form] (.getElementById js/document "container"))
```

The form can be initialized with a populated document, and the fields will be initialize with the values found there:

```clojure
(def form-template
  [:div
   (row "first name"
        [:input.form-control {:field :text :id :first-name}])
   (row "last name"
        [:input.form-control {:field :text :id :last-name}])
   (row "age"
        [:input {:field.form-control :numeric :id :age}])
   (row "email"
        [:input {:field.form-control :email :id :email}])
   (row "comments"
        [:textarea.form-control {:field :textarea :id :comments}]))

(defn form []
  (let [doc (atom {:first-name "John" :last-name "Doe" :age 35})]
    (fn []
      [:div
       [:div.page-header [:h1 "Reagent Form"]]
       [bind-fields form-template doc]
       [:label (str @doc)]])))
```

## Adding events

The `bind-fields` function accepts optional events. Events are triggered whenever the document is updated, and will be executed in the order they are listed. Each event sees the document modified by its predecessor.

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
   (row "Height" [:input {:field :numeric :id :height}])
   (row "Weight" [:input {:field :numeric :id :weight}])
   (row "BMI" [:input {:field :numeric :id :bmi :disabled true}])])

[bind-fields
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

## Using adapters

Adapters can be provided to fields so as to create custom storage formats for field values. These are a pair of functions passed to the field through the keys `:in-fn` and `:out-fn`. `:in-fn` modifies the stored item so that the field can make use of it while `:out-fn` modifies the output of the field before it is stored. For example, in order to use a native `js/Date` object as the storage format, the datepicker can be initialized thusly:

```clojure
[:div {:field :datepicker :id :birthday :date-format "yyyy/mm/dd" :inline true
       :in-fn #(when % {:year (.getFullYear %) :month (.getMonth %) :day (.getDate %)})
       :out-fn #(when % (js/Date (:year %) (:month %) (:day %)))}]
```

Adapters may be passed nulls so they must be able to handle those.

## Mobile Gotchas

React requires additional initialization in order to handle touch events:

```clojure
(.initializeTouchEvents js/React true)
```

Safari on iOS will have a 300ms delay for `:on-click` events, it's possible to set a custom trigger event using the `:touch-event` key. See [here](http://facebook.github.io/react/docs/events.html) for the list of events available in React. For example, if we wanted to use `:on-touch-start` instead of `:on-click` to trigger the event then we could do the following:

```clojure
[:input.form-control {:field :text :id :first-name :touch-event :on-touch-start}]
```

Note that you will also have to set the style of `cursor: pointer` for any elements other than buttons in order for events to work on iOS.

## License

Copyright © 2014 Yogthos

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
