# reagent-forms

A ClojureScript library to provide form data bindings for [Reagent](http://holmsand.github.io/reagent/), see [here](http://yogthos.github.io/reagent-forms-example.html) for a live demo.

## Install

[![Clojars Project](http://clojars.org/reagent-forms/latest-version.svg)](http://clojars.org/reagent-forms)

## Usage

The library uses a Reagent atom as the document store. The components are bound to the document using the `:field` attribute. This key will be used to decide how the specific type of component should be bound. The component must also provide a unique `:id` attribute that is used to correlate it to the document. While the library is geared towards usage with Twitter Bootstrap, it is fairly agnostic about the types of components that you create.

The `:id` can be a keyword, e.g: `{:id :foo}`, or a keywordized path `{:id :foo.bar}` that will map to `{:foo {:bar "value"}}`. Alternatively, you can specify a vector path explicitly `[:foo 0 :bar]`.

By default the component value is that of the document field, however all components support an `:in-fn` and `:out-fn` function attributes.
`:in-fn` accepts the current document value and returns what is to be displayed in the component. `:out-fn` accepts the component value
and returns what is to be stored in the document.

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

The typeahead field uses a `:data-source` key bound to a function that takes the current input and returns a list of matching results. The control uses an input element to handle user input and renders the list of choices as an  unordered list element containing one or more list item elements. Users may specify the css classes used to render each of these elements using the keys :input-class, :list-class and :item-class. Users may additionally specify a css class to handle highlighting of the current selection with the :highlight-class key. Reference css classes are included in the resources/public/css/reagent-forms.css file.

```clojure
(defn friend-source [text]
  (filter
    #(-> % (.toLowerCase %) (.indexOf text) (> -1))
    ["Alice" "Alan" "Bob" "Beth" "Jim" "Jane" "Kim" "Rob" "Zoe"]))

[:div {:field :typeahead
       :id :ta
       :input-placeholder "pick a friend"
       :data-source friend-source
       :input-class "form-control"
       :list-class "typeahead-list"
       :item-class "typeahead-item"
       :highlight-class "highlighted"}]
```

The typeahead field supports both mouse and keyboard selection.

##### Different display and value

You can make the input's displayed value be different to the value stored in the document. You need to specify `:out-fn`, a `:result-fn` and
optionally `:in-fn`. The `:data-source` needs to return a vector `[display-value stored-value]`.

```clojure
(defn people-source [people]
  (fn [text]
    (->> people
         (filter #(-> (:name %)
                      (.toLowerCase)
                      (.indexOf text)
                      (> -1)))
         (mapv #(vector (:name %) (:num %))))))

[:div {:field :typeahead
       :data-source (people-source people)
       :in-fn (fn [num]
                [(:name (first (filter #(= num (:num %)) people))) num])
       :out-fn (fn [[name num]] num)
       :result-fn (fn [[name num]] name)
       :id :author.num}]]]
```

##### Pop down the list

If `:data-source` responds with the full option list when passed the keyword `:all` then the down-arrow key will show the list.

##### Selection list from Ajax

The `:selections` attribute can be specified to pass an atom used to hold the selections. This gives the option to fetch the
list using typeahead text - if an ajax response handler sets the atom the list will pop down.

##### Display selection on pop-down

If supplied, the `:get-index` function will ensure the selected item is highlighted when the list is popped down.

A full example is available in the source code for the demonstration page.

#### :checkbox

The checkbox field creates a checkbox element:

```clojure
[:div.row
  [:div.col-md-2 "does data binding make you happy?"]
  [:div.col-md-5
   [:input.form-control {:field :checkbox :id :happy-bindings}]]]
```

The checkbox accepts an optional `:checked` attribute. When set the
checkbox will be selected and the document path pointed to by the `:id`
key will be set to `true`.

```clojure
[:div.row
  [:div.col-md-2 "does data binding make you happy?"]
  [:div.col-md-5
   [:input.form-control {:field :checkbox :id :happy-bindings :checked true}]]]
```

#### :range

Range control uses the `:min` and `:max` keys to create an HTML range input:

```clojure
[:input.form-control
 {:field :range :min 10 :max 100 :id :some-range}]
```

#### :radio

Radio buttons do not use the `:id` key since it must be unique and are instead grouped using the `:name` attribute. The `:value` attribute is used to indicate the value that is saved to the document:

```clojure
[:input {:field :radio :value :a :name :radioselection}]
[:input {:field :radio :value :b :name :radioselection}]
[:input {:field :radio :value :c :name :radioselection}]
```

The radio button accepts an optional `:checked` attribute. When set the
checkbox will be selected and the document path pointed to by the `:name` key
will be set to `true`.

```clojure
[:input {:field :radio :value :a :name :radioselection}]
[:input {:field :radio :value :b :name :radioselection :checked true}]
[:input {:field :radio :value :c :name :radioselection}]
```

#### :file

The file field binds the `File` object of an `<input type="file"/>`.

```clojure
[:input {:field :file :type :file}]
```

#### :files

Same as file, except it works with `<input type="file" multiple/>` and binds the entire `FileList` object.

```clojure
[:input {:field :file :type :file :multiple true}]
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
                        (= i 31) (some #{month} [:January :March :May :July :August :October :December])
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

Labels can be associated with a key in the document using the `:id` attribute and will display the value at that key. The lables can have an optional `:preamble` and `:postamble` keys with the text that will be rendered before and after the value respectively. The value can also be interpreted using a formatter function assigned to the `:fmt` key. The `:placeholder` key can be used to provide text that will be displayed in absence of a value:

```clojure
[:label {:field :label :id :volume}]
[:label {:field :label :preamble "the value is: " :id :volume}]
[:label {:field :label :preamble "the value is: " :postamble "ml" :id :volume}]
[:label {:field :label :preamble "the value is: " :postamble "ml" :placeholder "N/A" :id :volume}]
[:label {:field :label :preamble "the value is: " :id :volume :fmt (fn [v] (if v (str v "ml") "unknown")}]
```

#### :alert

Alerts are bound to an id of a field that triggers the alert and can have an optional `:event` key. The event key should point to a function that returns a boolean value.

An optional `:closeable? true/false` can be provided to control if a close button should be rendered (defaults to true).

When an event is supplied then the body of the alert is rendered whenever the event returns true:

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

The datepicker can also take an optional `:auto-close?` key to indicate that it should be closed when the day is clicked. This defaults to `false`.


The date format can be set using the `:date-format` key:

```Clojure
{:field :datepicker :id :date :date-format "yyyy/mm/dd"}
```

The `:date-format` can also point to a function that returns the formatted date:
```Clojure
{:field :datepicker
 :id :date
 :date-format (fn [{:keys [year month day]] (str year "/" month "/" day))}
```

The above is useful in conjunction with the `:save-fn` key that allows you to supply a custom function for saving the value.
For example, if you wanted to use a JavaScript date object, you could do the following:

```clojure
[:div.input-group.date.datepicker.clickable
 {:field       :datepicker
  :id          :reminder
  :date-format (fn [date]
                 (str (.getDate date) "/"
                      (inc (.getMonth date)) "/"
                      (.getFullYear date)))
  :save-fn     (fn [current-date {:keys [year month day]}]
                 (if current-date
                   (doto (js/Date.)
                     (.setFullYear year)
                     (.setMonth (dec month))
                     (.setDate day)
                     (.setHours (.getHours current-date))
                     (.setMinutes (.getMinutes current-date)))
                   (js/Date. year (dec month) day)))
  :auto-close? true}]
```

Note that you need to return a new date object in updates for the component to repaint.


Datepicker takes an optional `:lang` key which you can use to set the locale of the datepicker. There are currently English, Russian, German, French, Spanish, Portuguese, Finnish and Dutch built in translations. To use a built-in language pass in `:lang` with a keyword as in the following table:

| Language | Keyword |
|----------|---------|
| English | `:en-US` (default) |
| Russian | `:ru-RU` |
| German  | `:de-DE` |
| French  | `:fr-FR` |
| Spanish | `:es-ES` |
| Portuguese | `:pt-PT` |
| Finnish | `:fi-FI` |
| Dutch   | `:nl-NL` |

Example of using a built in language locale:

```Clojure
{:field :datepicker :id :date :date-format "yyyy/mm/dd" :inline true :lang :ru-RU}
```

You can also provide a custom locale hash-map to the datepicker. `:first-day` marks the first day of the week starting from Sunday as 0. All of the keys must be specified.

Example of using a custom locale hash-map:

```clojure
{:field :datepicker :id :date :date-format "yyyy/mm/dd" :inline true :lang
 {:days        ["First" "Second" "Third" "Fourth" "Fifth" "Sixth" "Seventh"]
  :days-short  ["1st" "2nd" "3rd" "4th" "5th" "6th" "7th"]
  :months      ["Month-one" "Month-two" "Month-three" "Month-four" "Month-five" "Month-six"
                "Month-seven" "Month-eight" "Month-nine" "Month-ten" "Month-eleven" "Month-twelve"]
  :months-short ["M1" "M2" "M3" "M4" "M5" "M6" "M7" "M8" "M9" "M10" "M12"]
  :first-day 0}}
```

The datepicker requires additional CSS in order to be rendered correctly. The default CSS is provided
in `reagent-forms.css` in the resource path. Simply make sure that it's included on the page.
The File can be read using:

```clojure
(-> "reagent-forms.css" clojure.java.io/resource slurp)
```

#### :container

The container element can be used to group different element.
The container can be used to set the visibility of multiple elements.

`:valid?` key accepts a function which takes the current state of the document as the sole argument. This function returns a class to be concatenated to the class list of the element.

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

The templates are eagerly evaluated, and you should always call the helper functions as in the example above instead of putting them in a vector. These will be replaced by Reagent components when the `bind-fields` is called to compile the template.

Once a form template is created it can be bound to a document using the `bind-fields` function:

```clojure
(ns myform.core
  (:require [reagent-forms.core :refer [bind-fields]]
            [reagent.core :as r]))

(defn form []
  (let [doc (r/atom {})]
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
        [:input.form-control {:field :numeric :id :age}])
   (row "email"
        [:input.form-control {:field :email :id :email}])
   (row "comments"
        [:textarea.form-control {:field :textarea :id :comments}])])

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

By default the options will contain the `get` and the `save!`, and `update!` keys. The `get` key points to a function that accepts an id and returns the document value associated with it. The `save!` function accepts an id and a value that will be associated with it. The `update!` function accepts an id, a function that will handle the update, and the value. The function handling the update will receive the old and the new values.

## Using adapters

Adapters can be provided to fields so as to create custom storage formats for field values. These are a pair of functions passed to the field through the keys `:in-fn` and `:out-fn`. `:in-fn` modifies the stored item so that the field can make use of it while `:out-fn` modifies the output of the field before it is stored. For example, in order to use a native `js/Date` object as the storage format, the datepicker can be initialized thusly:

```clojure
[:div {:field :datepicker :id :birthday :date-format "yyyy/mm/dd" :inline true
       :in-fn #(when % {:year (.getFullYear %) :month (.getMonth %) :day (.getDate %)})
       :out-fn #(when % (js/Date (:year %) (:month %) (:day %)))}]
```

Adapters may be passed nulls so they must be able to handle those.

## Mobile Gotchas

Safari on iOS will have a 300ms delay for `:on-click` events, it's possible to set a custom trigger event using the `:touch-event` key. See [here](http://facebook.github.io/react/docs/events.html) for the list of events available in React. For example, if we wanted to use `:on-touch-start` instead of `:on-click` to trigger the event then we could do the following:

```clojure
[:input.form-control {:field :text :id :first-name :touch-event :on-touch-start}]
```

Note that you will also have to set the style of `cursor: pointer` for any elements other than buttons in order for events to work on iOS.

The [TapEventPlugin](https://github.com/zilverline/react-tap-event-plugin) for react is another option for creating responsive events, until the functionality becomes available in React itself.

## Testing
This project uses [`Doo`](https://github.com/bensu/doo) for running the tests.
You must install one of the Doo-supported environments, refer to [the docs](https://github.com/bensu/doo#setting-up-environments) for details.
To run the tests, for example using Phantom, do:

```
lein doo phantom test
```

## License

Copyright © 2018 Dmitri Sotnikov

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
