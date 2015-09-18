(ns reagent-forms.core
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require
   [clojure.walk :refer [postwalk]]
   [clojure.string :refer [split trim]]
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
      (if (sequential? id)
        id
        (let [segments (split (subs (str id) 1 ) #"\.")]
          (map keyword segments))))))

(defn set-doc-value [doc id value events]
  (let [path (id->path id)]
    (reduce #(or (%2 path value %1) %1)
            (assoc-in doc path value)
            events)))

(defn- mk-save-fn [doc events]
  (fn [id value]
    (swap! doc set-doc-value id value events)))

(defn wrap-get-fn [get wrapper]
  (fn [id]
    (wrapper (get id))))

(defn wrap-save-fn [save! wrapper]
  (fn [id value]
    (save! id (wrapper value))))

(defn wrap-fns [opts node]
  {:doc (:doc opts)
   :get (if-let [in-fn (:in-fn (second node))]
          (wrap-get-fn (:get opts) in-fn)
          (:get opts))
   :save! (if-let [out-fn (:out-fn (second node))]
            (wrap-save-fn (:save! opts) out-fn)
            (:save! opts))})

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
        parsed))))

(defmethod format-type :default
  [_ value] value)

;;bind the field to the document based on its type
(defmulti bind
  (fn [{:keys [field]} _]
    (if (some #{field} [:text :numeric :password :email :tel :range :textarea])
      :input-field field)))

(defmethod bind :input-field
  [{:keys [field id fmt]} {:keys [get save! doc]}]
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
      (if (some #{field} [:range :text :password :email :tel :textarea])
        :input-field field))))

(defmethod init-field :container
  [[type {:keys [valid?] :as attrs} & body] {:keys [doc]}]
  (render-element attrs doc
    (into [type
           (if-let [valid-class (when valid? (valid? (deref doc)))]
             (update-in attrs [:class] #(if (not (empty? %)) (str % " " valid-class) valid-class))
             attrs)]
          body)))

(defmethod init-field :input-field
  [[_ {:keys [field] :as attrs} :as component] {:keys [doc] :as opts}]
  (render-element attrs doc
    (set-attrs component opts {:type field})))

(defmethod init-field :numeric
  [[type {:keys [id fmt] :as attrs}] {:keys [doc get save!]}]
  (let [input-value (atom nil)]
    (render-element attrs doc
      [type (merge
             {:type :text
              :value (or @input-value (get id ""))
              :on-change #(reset! input-value (value-of %))
              :on-blur #(do
                          (reset! input-value nil)
                          (->> (value-of %)
                               (format-value fmt)
                               (format-type :numeric)
                               (save! id)))}
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
          (merge
           {:read-only true
            :type :text
            :on-click #(swap! expanded? not)
            :value (when-let [date (get id)] (format-date date fmt))}
           attrs)]
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
  [[type {:keys [field name value] :as attrs} & body] {:keys [doc get save!]}]
  (render-element attrs doc
    (into
      [type
       (merge {:type :radio
               :checked (= value (get name))
               :on-change #(save! name value)}
              attrs)]
       body)))

(defmethod init-field :typeahead
  [[type {:keys [id data-source input-class list-class item-class highlight-class input-placeholder result-fn choice-fn clear-on-focus?]
          :as attrs
          :or {result-fn identity
               choice-fn identity
               clear-on-focus? true}}] {:keys [doc get save!]}]
  (let [typeahead-hidden? (atom true)
        mouse-on-list? (atom false)
        selected-index (atom 0)
        selections (atom [])
        choose-selected #(when (not-empty @selections)
                           (let [choice (nth @selections @selected-index)]
                             (save! id choice)
                             (choice-fn choice)
                             (reset! typeahead-hidden? true)))]
    (render-element attrs doc
                    [type
                     [:input {:type        :text
                              :placeholder input-placeholder
                              :class       input-class
                              :value       (let [v (get id)]
                                             (if-not (iterable? v)
                                               v (first v)))
                              :on-focus    #(when clear-on-focus? (save! id nil))
                              :on-blur     #(when-not @mouse-on-list?
                                              (reset! typeahead-hidden? true)
                                              (reset! selected-index 0))
                              :on-change   #(when-let [value (trim (value-of %))]
                                              (reset! selections (data-source (.toLowerCase value)))
                                              (save! id (value-of %))
                                              (reset! typeahead-hidden? false)
                                              (reset! selected-index 0))
                              :on-key-down #(do
                                              (case (.-which %)
                                                38 (do
                                                     (.preventDefault %)
                                                     (if-not (= @selected-index 0)
                                                       (swap! selected-index dec)))
                                                40 (do
                                                     (.preventDefault %)
                                                     (if-not (= @selected-index (dec (count @selections)))
                                                       (swap! selected-index inc)))
                                                9  (choose-selected)
                                                13 (choose-selected)
                                                27 (do (reset! typeahead-hidden? true)
                                                       (reset! selected-index 0))
                                                "default"))}]

                     [:ul {:style {:display (if (or (empty? @selections) @typeahead-hidden?) :none :block) }
                           :class list-class
                           :on-mouse-enter #(reset! mouse-on-list? true)
                           :on-mouse-leave #(reset! mouse-on-list? false)}
                      (doall
                       (map-indexed
                        (fn [index result]
                          [:li {:tab-index     index
                                :key           index
                                :class         (if (= @selected-index index) highlight-class item-class)
                                :on-mouse-over #(do
                                                  (reset! selected-index (js/parseInt (.getAttribute (.-target %) "tabIndex"))))
                                :on-click      #(do
                                                  (reset! typeahead-hidden? true)
                                                  (save! id result)
                                                  (choice-fn result))}
                           (result-fn result)])
                        @selections))]])))



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
        selectors  (map (fn [item]
                          {:visible? (:visible? (second item))
                           :selector [(group-item item opts selections field id)]})
                        selection-items)]
    (fn []
      (when-not (get id)
        (swap! selections #(into {} (map (fn [[k]] [k false]) %))))
      (into [type attrs]
            (->> selectors
                  (filter
                   #(if-let [visible? (:visible? %)]
                      (visible? @(:doc opts)) true))
                  (map :selector))))))

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
                   (let [opts (wrap-fns opts node)
                         field (init-field node opts)]
                     (if (fn? field) [field] field))
                   node))
               form)]
    (fn [] form)))
