(ns reagent-forms.core
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require
   [clojure.walk :refer [postwalk]]
   [clojure.string :refer [split trim join blank?]]
   [goog.string :as gstring]
   [goog.string.format]
   [reagent.core :as r :refer [atom cursor]]
   [reagent-forms.datepicker
    :refer [parse-format format-date datepicker]]))

(defn value-of [element]
  (-> element .-target .-value))

(defn- scroll-to [element idx]
  (let [list-elem (-> element
                      .-target
                      .-parentNode
                      (.getElementsByTagName "ul")
                      (.item 0))
        idx (if (< idx 0) 0 idx)
        item-elem (-> list-elem
                      .-children
                      (.item idx))
        [item-height offset-top] (if item-elem
                                   [(.-scrollHeight item-elem)
                                    (.-offsetTop item-elem)]
                                   [0 0])]
    (set! (.-scrollTop list-elem)
          (- offset-top
             (* 2 item-height)))))

(def ^:private id->path
  (memoize
    (fn [id]
      (if (sequential? id)
        id
        (let [segments (split (subs (str id) 1 ) #"\.")]
          (map keyword segments))))))

(def ^:private cursor-for-id
  (memoize
    (fn [doc id]
      (cursor doc (id->path id)))))

(defn run-events [doc id value events]
  (let [path (id->path id)]
    (reduce #(or (%2 path value %1) %1) doc events)))

(defn- mk-update-fn [doc events]
  (fn [id update-fn value]
    (let [result (swap! (cursor-for-id doc id) update-fn value)]
      (when-not (empty? events)
        (swap! doc run-events id result events)))))

(defn- mk-save-fn [doc events]
  (fn [id value]
    (reset! (cursor-for-id doc id) value)
    (when-not (empty? events)
      (swap! doc run-events id value events))))

(defn wrap-get-fn [get wrapper]
  (fn [id]
    (wrapper (get id))))

(defn wrap-save-fn [save! wrapper]
  (fn [id value]
    (save! id (wrapper value))))

(defn wrap-update-fn [update! wrapper]
  (fn [id update-fn value]
    (update! id update-fn (wrapper value))))

(defn wrap-fns [{:keys [doc get save! update!]} node]
  {:doc doc
   :get (if-let [in-fn (:in-fn (second node))]
          (wrap-get-fn get in-fn)
          get)
   :save! (if-let [out-fn (:out-fn (second node))]
            (wrap-save-fn save! out-fn)
            save!)
   :update! (if-let [out-fn (:out-fn (second node))]
              (wrap-update-fn update! out-fn)
              update!)})

(defn clean-attrs [attrs]
  (dissoc attrs
          :fmt
          :event
          :field
          :inline
          :save-fn
          :preamble
          :postamble
          :visible?
          :date-format
          :auto-close?))

;;coerce the input to the appropriate type
(defmulti format-type
  (fn [field-type _]
    (if (#{:range :numeric} field-type)
      :numeric
      field-type)))

(defn format-value [fmt value]
  (if (and (not (js/isNaN (js/parseFloat value))) fmt)
    (gstring/format fmt value)
    value))

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
    (if (#{:text :numeric :password :email :tel :range :textarea} field)
      :input-field field)))

(defmethod bind :input-field
  [{:keys [field id fmt]} {:keys [get save!]}]
  {:value (let [value (or (get id) "")]
            (format-value fmt value))
   :on-change #(save! id (->> % (value-of) (format-type field)))})

(defmethod bind :checkbox
  [{:keys [id]} {:keys [get save!]}]
  {:checked   (boolean (get id))
   :on-change #(->> id get not (save! id))})

(defmethod bind :default [_ _])

(defn- set-attrs
  [[type attrs & body] opts & [default-attrs]]
  (into
    [type (clean-attrs
            (merge
              default-attrs
              (bind attrs opts)
              (dissoc attrs :checked :default-checked)))]
    body))

;;initialize the field by binding it to the document and setting default options
(defmulti init-field
  (fn [[_ {:keys [field]}] _]
    (let [field (keyword field)]
      (if (#{:range :text :password :email :tel :textarea} field)
        :input-field field))))

(defmethod init-field :container
  [[type {:keys [valid?] :as attrs} & body] {:keys [doc]}]
  (render-element attrs doc
    (into [type
           (clean-attrs
             (if-let [valid-class (when valid? (valid? (deref doc)))]
             (update-in attrs [:class] #(if (not (empty? %)) (str % " " valid-class) valid-class))
             attrs))]
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
             (clean-attrs attrs))])))

(defmethod init-field :datepicker
  [[_ {:keys [id date-format inline auto-close? disabled lang save-fn] :or {lang :en-US} :as attrs}] {:keys [doc get save! update!]}]
  (let [fmt (if (fn? date-format)
              date-format
              #(format-date % (parse-format date-format)))
        selected-date (get id)
        selected-month (if (pos? (:month selected-date))
                         (dec (:month selected-date))
                         (:month selected-date))
        today (js/Date.)
        year (or (:year selected-date) (.getFullYear today))
        month (or selected-month (.getMonth today))
        day (or (:day selected-date) (.getDate today))
        expanded? (atom false)
        mouse-on-list? (atom false)
        dom-node (atom nil)
        save-value (if save-fn #(update! id save-fn %) #(save! id %))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (->> this r/dom-node .-firstChild .-firstChild (reset! dom-node)))
       :component-did-update
       (fn [this]
         (->> this r/dom-node .-firstChild .-firstChild (reset! dom-node)))
       :render
       (render-element attrs doc
        [:div.datepicker-wrapper
         [:div.input-group.date
           [:input.form-control
            (merge
             {:read-only true
              :on-blur #(when-not @mouse-on-list?
                          (reset! expanded? false))
              :type :text
              :on-click (fn [e]
                           (.preventDefault e)
                           (when-not (if (fn? disabled) (disabled) disabled)
                             (swap! expanded? not)))
              :value (if-let [date (get id)]
                       (fmt date)
                       "")}
             (clean-attrs attrs))]
           [:span.input-group-addon
            {:on-click (fn [e]
                          (.preventDefault e)
                          (when-not (if (fn? disabled) (disabled) disabled)
                            (swap! expanded? not)
                            (.focus @dom-node)))}
            [:i.glyphicon.glyphicon-calendar]]]
         [datepicker year month day dom-node mouse-on-list? expanded? auto-close? #(get id) save-value inline lang]])})))


(defmethod init-field :checkbox
  [[_ {:keys [id field checked default-checked] :as attrs} :as component] {:keys [doc get save!] :as opts}]
  (when (or checked default-checked)
    (save! id true))
  (render-element (dissoc attrs :checked :default-checked) doc
      (set-attrs component opts {:type field})))

(defmethod init-field :label
  [[type {:keys [id preamble postamble placeholder fmt] :as attrs}] {:keys [doc get]}]
  (render-element attrs doc
    [type (clean-attrs attrs) preamble
     (let [value (get id)]
       (if fmt
         (fmt value)
         (if value
           (str value postamble)
           placeholder)))]))

(defmethod init-field :alert
  [[type {:keys [id event touch-event closeable?] :or {closeable? true} :as attrs} & body] {:keys [doc get save!] :as opts}]
  (render-element attrs doc
    (if event
      (when (event (get id))
        (into [type (clean-attrs attrs)] body))
      (if-let [message (not-empty (get id))]
        [type (clean-attrs attrs)
         (when closeable?
           [:button.close
            {:type                      "button"
             :aria-hidden               true
             (or touch-event :on-click) #(save! id nil)}
            "X"])
         message]))))

(defmethod init-field :radio
  [[type {:keys [field name value checked default-checked] :as attrs} & body] {:keys [doc get save!]}]
  (when (or checked default-checked)
    (save! name value))
  (render-element attrs doc
    (into
      [type
       (merge
         (dissoc (clean-attrs attrs) :value :default-checked)
         {:type :radio
          :checked (= value (get name))
          :on-change #(save! name value)})]
       body)))

(defmethod init-field :typeahead
  [[type {:keys [id data-source input-class list-class item-class highlight-class input-placeholder result-fn choice-fn clear-on-focus? selections get-index]
          :as attrs
          :or {result-fn identity
               choice-fn identity
               clear-on-focus? true}}] {:keys [doc get save!]}]
  (let [typeahead-hidden? (atom true)
        mouse-on-list? (atom false)
        selected-index (atom -1)
        selections (or selections (atom []))
        get-index (or get-index (constantly -1))
        choose-selected #(when (and (not-empty @selections) (> @selected-index -1))
                           (let [choice (nth @selections @selected-index)]
                             (save! id choice)
                             (choice-fn choice)
                             (reset! typeahead-hidden? true)))]
    (render-element attrs doc
                    [type
                     [:input {:type        :text
                              :disabled    (:disabled attrs)
                              :placeholder input-placeholder
                              :class       input-class
                              :value       (let [v (get id)]
                                             (if-not (iterable? v)
                                               v (first v)))
                              :on-focus    #(when clear-on-focus? (save! id nil))
                              :on-blur     #(when-not @mouse-on-list?
                                              (reset! typeahead-hidden? true)
                                              (reset! selected-index -1))
                              :on-change   #(when-let [value (trim (value-of %))]
                                              (reset! selections (data-source (.toLowerCase value)))
                                              (save! id (value-of %))
                                              (reset! typeahead-hidden? false)
                                              (reset! selected-index (if (= 1 (count @selections)) 0 -1)))
                              :on-key-down #(do
                                              (case (.-which %)
                                                38 (do
                                                     (.preventDefault %)
                                                     (when-not (or @typeahead-hidden? (<= @selected-index 0))
                                                       (swap! selected-index dec)
                                                       (scroll-to % @selected-index)))
                                                40 (do
                                                     (.preventDefault %)
                                                     (if @typeahead-hidden?
                                                       (do

                                                         (reset! selections (data-source :all))
                                                         (reset! selected-index (get-index (-> %
                                                                                               value-of
                                                                                               trim)
                                                                                           @selections))
                                                         (reset! typeahead-hidden? false)
                                                         (scroll-to % @selected-index))
                                                       (when-not (= @selected-index (dec (count @selections)))
                                                         (save! id (value-of %))
                                                         (swap! selected-index inc)
                                                         (scroll-to % @selected-index))))
                                                9 (choose-selected)
                                                13 (do
                                                     (.preventDefault %)
                                                     (choose-selected))
                                                27 (do (reset! typeahead-hidden? true)
                                                       (reset! selected-index -1))
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
                                                  (.preventDefault %)
                                                  (reset! typeahead-hidden? true)
                                                  (save! id result)
                                                  (choice-fn result))}
                           (result-fn result)])
                        @selections))]])))

(defmethod init-field :file
  [[type {:keys [id] :as attrs}] {:keys [doc save!]}]
  (render-element attrs doc
    [type (merge {:type :file
                  :on-change #(save! id (-> % .-target .-files array-seq first))}
                 (clean-attrs attrs))]))

(defmethod init-field :files
  [[type {:keys [id] :as attrs}] {:keys [doc save!]}]
  (render-element attrs doc
    [type (merge {:type :file
                  :multiple true
                  :on-change #(save! id (-> % .-target .-files))}
                 (clean-attrs attrs))]))

(defn- group-item
  [[type {:keys [key touch-event disabled] :as attrs} & body]
   {:keys [save! multi-select]} selections field id]
  (letfn [(handle-click! []
           (if multi-select
             (do
               (swap! selections update-in [key] not)
               (save! id (->> @selections (filter second) (map first))))
             (let [value (get @selections key)]
               (reset! selections {key (not value)})
               (save! id (when (get @selections key) key)))))]
    (fn []
      (let [disabled? (if (fn? disabled) (disabled) disabled)
            active? (get @selections key)
            button-or-input? (let [t (subs (name type) 0 5)]
                               (or (= t "butto") (= t "input")))
            class (->> [(when active? "active")
                        (when (and disabled? (not button-or-input?)) "disabled")]
                       (remove blank?)
                       (join " "))]
        [type
         (dissoc
           (merge {:class class
                   (or touch-event :on-click)
                   (when-not disabled? handle-click!)}
                  (clean-attrs attrs)
                  {:disabled disabled?})
           (when-not button-or-input? :disabled))
         body]))))

(defn- mk-selections [id selectors {:keys [get multi-select] :as ks}]
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
      (into [type (clean-attrs attrs)]
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
       (merge
         (clean-attrs attrs)
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

(defn make-form
  [form opts wrap-fns?]
  (postwalk
    (fn [node]
      (if (field? node)
        (let [opts (if wrap-fns? (wrap-fns opts node) opts)
              field (init-field node opts)]
          (if (fn? field) [field] field))
        node))
    form))

(defmulti bind-fields
  "Creates data bindings between the form fields and the supplied atom or calls
   the supplied functions (when `doc` is a map) on events triggered by fields.
   form - the form template with the fields
   doc - the document that the fields will be bound to
   events - any events that should be triggered when the document state changes"
  (fn [_ doc & _]
    (type doc)))

(defmethod bind-fields PersistentArrayMap
  [form doc]
  (let [form (make-form form (assoc doc :doc (:get doc)) false)]
    (fn [] form)))

(defmethod bind-fields :default
  [form doc & events]
  (let [opts {:doc doc
              :get #(deref (cursor-for-id doc %))
              :save! (mk-save-fn doc events)
              :update! (mk-update-fn doc events)}
        form (make-form form opts true)]
    (fn [] form)))
