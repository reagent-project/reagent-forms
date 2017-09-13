(ns reagent-forms.page
  (:require
    [json-html.core :refer [edn->hiccup]]
    [reagent.core :as r]
    [reagent-forms.core :as forms]))


(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn radio [label name value]
  [:div.radio
   [:label
    [:input {:field :radio :name name :value value}]
    label]])

(defn input [label type id]
  (row label [:input.form-control {:field type :id id}]))

(defn friend-source [text]
  (filter
    #(-> % (.toLowerCase %) (.indexOf text) (> -1))
    ["Alice" "Alan" "Bob" "Beth" "Jim" "Jane" "Kim" "Rob" "Zoe"]))

(def animals
  [{:Animal {:Name "Lizard"
             :Colour "Green"
             :Skin   "Leathery"
             :Weight 100
             :Age 10
             :Hostile false}}
   {:Animal {:Name "Lion"
             :Colour "Gold"
             :Skin   "Furry"
             :Weight 190000
             :Age 4
             :Hostile true}}
   {:Animal {:Name "Giraffe"
             :Colour "Green"
             :Skin   "Hairy"
             :Weight 1200000
             :Age 8
             :Hostile false}}
   {:Animal {:Name "Cat"
             :Colour "Black"
             :Skin   "Furry"
             :Weight 5500
             :Age 6
             :Hostile false}}
   {:Animal {:Name "Capybara"
             :Colour "Brown"
             :Skin   "Hairy"
             :Weight 45000
             :Age 12
             :Hostile false}}
   {:Animal {:Name "Bear"
             :Colour "Brown"
             :Skin   "Furry"
             :Weight 600000
             :Age 10
             :Hostile true}}
   {:Animal {:Name "Rabbit"
             :Colour "White"
             :Skin   "Furry"
             :Weight 1000
             :Age 6
             :Hostile false}}
   {:Animal {:Name "Fish"
             :Colour "Gold"
             :Skin   "Scaly"
             :Weight 50
             :Age 5
             :Hostile false}}
   {:Animal {:Name "Hippo"
             :Colour "Grey"
             :Skin   "Leathery"
             :Weight 1800000
             :Age 10
             :Hostile false}}
   {:Animal {:Name "Zebra"
             :Colour "Black/White"
             :Skin   "Hairy"
             :Weight 200000
             :Age 9
             :Hostile false}}
   {:Animal {:Name "Squirrel"
             :Colour "Grey"
             :Skin   "Furry"
             :Weight 300
             :Age 1
             :Hostile false}}
   {:Animal {:Name "Crocodile"
             :Colour "Green"
             :Skin   "Leathery"
             :Weight 500000
             :Age 10
             :Hostile true}}])

(defn- animal-text
  "Return the display text for an animal"
  [animal]
  (str (:Name animal) " [" (:Colour animal) " " (:Skin animal) "]"))

(defn- animal-match
  "Return true if the given text is found in one of
  the Name, Skin or Colour fields. False otherwise"
  [animal text]
  (let [fields [:Name :Colour :Skin]
        text (.toLowerCase text)]
    (reduce (fn [_ field]
              (if (-> animal
                      field
                      .toLowerCase
                      (.indexOf text)
                      (> -1))
                (reduced true)
                false))
            false
            fields)))

(defn- animal-list
  "Generate the list of matching instruments for the given input list
  and match text.
  Returns a vector of vectors for a reagent-forms data-source."
  [animals text]
  (->> animals
       (filter #(-> %
                    :Animal
                    (animal-match text)))
       (mapv #(vector (animal-text (:Animal %)) (:Animal %)))))

(defn- get-item-index
  "Return the index of the specified item within the current selections.
  The selections is the vector returned by animal-source. Item is whatever
  the the document id is, or the in-fn returns, if there is one."
  [item selections]
  (first (keep-indexed (fn [idx animal]
                         (when (animal-match
                                 (second animal)
                                 item)
                           idx))
                       selections)))

(defn- animal-source
  [doc text]
  (cond
    (= text :all)
    (animal-list animals "")

    :else
    (animal-list animals text)))

(defn- animal-out-fn
  "The reagent-forms :out-fn for the animal chooser. We use the out-fn to
  store the animal object in the document and return just the name for display
  in the component."
  [doc val]
  (let [[animal-display animal] val] ; may be
    (if (:Name animal)
      (do
        (swap! doc #(assoc % :animal animal))
        (:Name animal))
      (do
        (swap! doc #(assoc % :animal nil))
        val))))

(defn form-template
  [doc]
  [:div
   (input "first name" :text :person.first-name)
   [:div.row
    [:div.col-md-2]
    [:div.col-md-5
     [:div.alert.alert-danger
      {:field :alert :id :errors.first-name}]]]

   (input "last name" :text :person.last-name)
   [:div.row
    [:div.col-md-2]
    [:div.col-md-5
     [:div.alert.alert-success
      {:field :alert :id :person.last-name :event empty?}
      "last name is empty!"]]]

   [:div.row
    [:div.col-md-2 [:label "Age"]]
    [:div.col-md-5
     [:div
      [:label
       [:input
        {:field :datepicker :id :age :date-format "yyyy/mm/dd" :inline true}]]]]]

   (input "email" :email :person.email)
   (row
     "comments"
     [:textarea.form-control
      {:field :textarea :id :comments}])

   [:hr]
   (input "kg" :numeric :weight-kg)
   (input "lb" :numeric :weight-lb)

   [:hr]
   [:h3 "BMI Calculator"]
   (input "height" :numeric :height)
   (input "weight" :numeric :weight)
   (row "BMI"
        [:input.form-control
         {:field :numeric :fmt "%.2f" :id :bmi :disabled true}])
   [:hr]

   (row "Best friend"
        [:div {:field             :typeahead
               :id                :ta
               :data-source       friend-source
               :input-placeholder "Who's your best friend? You can pick only one"
               :input-class       "form-control"
               :list-class        "typeahead-list"
               :item-class        "typeahead-item"
               :highlight-class   "highlighted"}])
   [:br]

   (row "isn't data binding lovely?"
        [:input {:field :checkbox :id :databinding.lovely}])
   [:label
    {:field       :label
     :preamble    "The level of awesome: "
     :placeholder "N/A"
     :id          :awesomeness}]

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
     :foo :a)
   (radio
     "Option two can be something else and selecting it will deselect option one"
     :foo :b)

   [:hr]

   (row "Big typeahead example (down arrow shows list)"
        [:div
         {:field             :typeahead
          :id                [:animal-text]
          :input-placeholder "Animals"
          :data-source       (fn [text] (animal-source doc text))
          :result-fn         (fn [[animal-display animal]] animal-display)
          :out-fn            (fn [val] (animal-out-fn doc val))
          :get-index         (fn [item selections] (get-item-index item selections))
          :clear-on-focus?   false
          :input-class       "form-control"
          :list-class        "typeahead-list"
          :item-class        "typeahead-item"
          :highlight-class   "highlighted"}
          ])

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
   [:div.list-group {:field :single-select :id :pick-one}
    [:div.list-group-item {:key :foo} "foo"]
    [:div.list-group-item {:key :bar} "bar"]
    [:div.list-group-item {:key :baz} "baz"]]

   [:h3 "multi-select list"]
   [:ul.list-group {:field :multi-select :id :pick-a-few}
    [:li.list-group-item {:key :foo} "foo"]
    [:li.list-group-item {:key :bar} "bar"]
    [:li.list-group-item {:key :baz} "baz"]]])

(defn home-page []
  (let [doc (r/atom {:person         {:first-name "John"
                                      :age        35
                                      :email      "foo@bar.baz"}
                     :weight         100
                     :height         200
                     :bmi            0.5
                     :comments       "some interesting comments\non this subject"
                     :radioselection :b
                     :position       [:left :right]
                     :pick-one       :bar
                     :unique         {:position :middle}
                     :pick-a-few     [:bar :baz]
                     :many           {:options :bar}
                     :animal-text    ""
                     :animal         nil})]
    (fn []
      [:div
       [:div.page-header [:h1 "Sample Form"]]

       [forms/bind-fields
        (form-template doc)
        doc
        (fn [[id] value {:keys [weight-lb weight-kg] :as document}]
          (cond
            (= id :weight-lb)
            (assoc document :weight-kg (/ value 2.2046))
            (= id :weight-kg)
            (assoc document :weight-lb (* value 2.2046))
            :else nil))
        (fn [[id] value {:keys [height weight] :as document}]
          (when (and (some #{id} [:height :weight]) weight height)
            (assoc document :bmi (/ weight (* height height)))))]

       [:button.btn.btn-default
        {:on-click
         #(if (empty? (get-in @doc [:person :first-name]))
            (swap! doc assoc-in [:errors :first-name] "first name is empty"))}
        "save"]

       [:hr]
       [:h1 "Document State"]
       [edn->hiccup @doc]])))

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))


(defn init! []
  (mount-root))
