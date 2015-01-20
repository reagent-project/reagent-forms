(ns reagent-forms.core
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require
   [clojure.walk :refer [postwalk]]
   [clojure.string :refer [split]]
   [goog.string :as gstring]
   [goog.string.format]
   [reagent.core :as reagent :refer [atom]]
   [reagent-forms.datepicker
    :refer [parse-format format-date datepicker]]))

(defn value-of [element]
 (-> element .-target .-value))

(def ^:private id->path
  (memoize
    (fn [id]
      (map keyword (-> id name (split  #"\."))))))

(defn set-doc-value [doc id value events]
  (let [path (id->path id)]
    (reduce #(or (%2 path value %1) %1)
            (assoc-in doc path value)
            events)))

(defn- mk-save-fn [doc events]
  (fn [id value]
    (swap! doc set-doc-value id value events)))

;;coerce the input to the appropriate type
(defmulti format-type
  (fn [field-type _]
    (if (some #{field-type} [:range :numeric])
      :numeric
      field-type)))

(defn valid-number-ending? [n]
  (or (and (not= "." (last (butlast n))) (= "." (last n)))
      (= "0" (last n))))

(defn format-value [fmt value]
  (if (and (not (js/isNaN (js/parseFloat value))) fmt) (gstring/format fmt value) value))

(defmethod format-type :numeric
  [_ n]
  (when (not-empty n)
    (let [parsed (js/parseFloat n)]
      (when-not (js/isNaN parsed)
        (if (valid-number-ending? n)
          n parsed)))))

(defmethod format-type :default
  [_ value] value)

;;bind the field to the document based on its type
(defmulti bind
  (fn [{:keys [field]} _]
    (if (some #{field} [:text :numeric :password :email :range :textarea])
      :input-field field)))

(defmethod bind :input-field
  [{:keys [field id fmt]} {:keys [get save!]}]
  {:value (let [value (or (get id) "")]
            (format-value fmt value))
   :on-change #(save! id (->> % (value-of) (format-type field)))})

(defmethod bind :checkbox
  [{:keys [id]} {:keys [get save!]}]
  {:checked (get id)
   :on-change #(->> id get not (save! id))})

(defmethod bind :default [_ _])

(defn- set-attrs
  [[type attrs & body] opts & [default-attrs]]
  (into [type (merge default-attrs (bind attrs opts) attrs)] body))

;;initialize the field by binding it to the document and setting default options
(defmulti init-field
  (fn [[_ {:keys [field]}] _]
    (let [field (keyword field)]
      (if (some #{field} [:range :text :password :email :textarea])
        :input-field field))))

(defmethod init-field :container
  [[_ attrs :as component] {:keys [doc]}]
  (render-element attrs doc component))

(defmethod init-field :input-field
  [[_ {:keys [field] :as attrs} :as component] {:keys [doc] :as opts}]
  (render-element attrs doc
    (set-attrs component opts {:type field})))

(defmethod init-field :numeric
  [[type {:keys [id fmt] :as attrs}] {:keys [doc get save!]}]
  (let [display-value (atom {:changed-self? false :value (get id)})]
    (render-element attrs doc
      [type (merge
             {:type :text
              :value
              (let [doc-value (or (get id) "")
                    {:keys [changed-self? value]} @display-value
                    value (if changed-self? value doc-value)]
                (swap! display-value dissoc :changed-self?)
                (format-value fmt value))
              :on-change
              #(if-let [value (format-type :numeric (value-of %))]
                 (do
                   (reset! display-value {:changed-self? true :value value})
                   (save! id (js/parseFloat value)))
                 (save! id nil))}
             attrs)])))

(defmethod init-field :datepicker
  [[_ {:keys [id date-format inline auto-close?] :as attrs}] {:keys [doc get save!]}]
  (let [fmt (parse-format date-format)
        today (js/Date.)
        expanded? (atom false)]
    (render-element attrs doc
      [:div.datepicker-wrapper
       [:div.input-group.date
         [:input.form-control
          {:read-only true
           :type :text
           :on-click #(swap! expanded? not)
           :value (when-let [date (get id)] (format-date date fmt))}]
         [:span.input-group-addon
          {:on-click #(swap! expanded? not)}
          [:i.glyphicon.glyphicon-calendar]]]
       [datepicker (.getFullYear today) (.getMonth today) (.getDate today) expanded? auto-close? #(get id) #(save! id %) inline]])))


(defmethod init-field :checkbox
  [[_ {:keys [id field] :as attrs} :as component] {:keys [doc get] :as opts}]
  (render-element attrs doc
      (set-attrs component opts {:type field})))

(defmethod init-field :label
  [[type {:keys [id preamble postamble placeholder] :as attrs}] {:keys [doc get]}]
  (render-element attrs doc
    [type attrs preamble
     (if-let [value (get id)]
       (str value postamble)
       placeholder)]))

(defmethod init-field :alert
  [[type {:keys [id event touch-event] :as attrs} & body] {:keys [doc get save!] :as opts}]
  (render-element attrs doc
    (if event
      (when (event (get id))
        (into [type (dissoc attrs event)] body))
      (if-let [message (not-empty (get id))]
        [type attrs
         [:button.close
           {:type                      "button"
            :aria-hidden               true
            (or touch-event :on-click) #(save! id nil)}
           "X"]
         message]))))

(defmethod init-field :radio
  [[type {:keys [field id value] :as attrs} & body] {:keys [doc get save!]}]
  (let [state (atom (= value (get id)))]
    (render-element attrs doc
      (into
        [type
         (merge {:type :radio
                 :checked @state
                 :on-change
                 #(do
                    (save! id value)
                    (reset! state (= value (get id))))}
                attrs)]
         body))))

(defmethod init-field :typeahead
  [[type {:keys [id data-source] :as attrs}] {:keys [doc get save!]}]
  (let [typeahead-hidden? (atom true)
        mouse-on-list? (atom false)]
    (render-element attrs doc
      [type
       [:input {:type      :text
                :value     (get id)
                :on-blur   #(when-not @mouse-on-list?
                             (reset! typeahead-hidden? true))
                :on-change #(do
                             (save! id (value-of %))
                             (reset! typeahead-hidden? false))}]
       (when-let [value (get id)]
         (let [results (data-source (.toLowerCase value))]
           [:ul.typeahead {:hidden         (or (empty? results) @typeahead-hidden?)
                           :on-mouse-enter #(reset! mouse-on-list? true)
                           :on-mouse-leave #(reset! mouse-on-list? false)}
            (for [result results]
              [:li {:on-click #(do
                                (reset! typeahead-hidden? true)
                                (save! id result))} result])]))])))

(defn- group-item [[type {:keys [key touch-event] :as attrs} & body] {:keys [save! multi-select]} selections field id]
  (letfn [(handle-click! []
           (if multi-select
             (do
               (swap! selections update-in [key] not)
               (save! id (->> @selections (filter second) (map first))))
             (let [value (get @selections key)]
               (reset! selections {key (not value)})
               (save! id (when (get @selections key) key)))))]

    (fn []
      [type (merge {:class (if (get @selections key) "active")
                    (or touch-event :on-click) handle-click!} attrs) body])))

(defn- mk-selections [id selectors {:keys [get multi-select]}]
  (let [value (get id)]
    (reduce
     (fn [m [_ {:keys [key]}]]
       (assoc m key (boolean (some #{key} (if multi-select value [value])))))
     {} selectors)))

(defn extract-selectors
  "selectors might be passed in inline or as a collection"
  [selectors]
  (if (keyword? (ffirst selectors))
    selectors (first selectors)))

(defn- selection-group
  [[type {:keys [field id] :as attrs} & selection-items] {:keys [get] :as opts}]
  (let [selection-items (extract-selectors selection-items)
        selections (atom (mk-selections id selection-items opts))
        selectors (map (fn [item]
                         {:visible? (:visible? (second item))
                          :selector [(group-item item opts selections field id)]})
                       selection-items)]
    (fn []
      (when-not (get id)
        (swap! selections #(into {} (map (fn [[k]] [k false]) %))))
      [type
       attrs
       (->> selectors
           (filter
           #(if-let [visible? (:visible? %)]
             (visible? @(:doc opts)) true))
           (map :selector)
           (doall))])))

(defmethod init-field :single-select
  [[_ attrs :as field] {:keys [doc] :as opts}]
  (render-element attrs doc
    [selection-group field opts]))

(defmethod init-field :multi-select
  [[_ attrs :as field] {:keys [doc] :as opts}]
  (render-element attrs doc
    [selection-group field (assoc opts :multi-select true)]))

(defn map-options [options]
  (into
   {}
   (for [[_ {:keys [key]} label] options]
     [(str label) key])))

(defn default-selection [options v]
  (->> options
       (filter #(= v (get-in % [1 :key])))
       (first)
       (last)))

(defmethod init-field :list
  [[type {:keys [field id] :as attrs} & options] {:keys [doc get save!]}]
  (let [options (extract-selectors options)
        options-lookup (map-options options)
        selection (atom (or
                         (get id)
                         (get-in (first options) [1 :key])))]
    (save! id @selection)
    (render-element attrs doc
      [type
       (merge attrs
              {:default-value (default-selection options @selection)
               :on-change #(save! id (clojure.core/get options-lookup (value-of %)))})
       (doall
         (filter
           #(if-let [visible? (:visible? (second %))]
             (visible? @doc) true)
           options))])))

(defn- field? [node]
  (and (coll? node)
       (map? (second node))
       (contains? (second node) :field)))

(defn bind-fields
  "creates data bindings between the form fields and the supplied atom
   form - the form template with the fields
   doc - the document that the fields will be bound to
   events - any events that should be triggered when the document state changes"
  [form doc & events]
  (let [opts {:doc doc
              :get #(get-in @doc (id->path %))
              :save! (mk-save-fn doc events)}
        form (postwalk
               (fn [node]
                 (if (field? node)
                   (let [field (init-field node opts)]
                     (if (fn? field) [field] field))
                   node))
               form)]
    (fn [] form)))
