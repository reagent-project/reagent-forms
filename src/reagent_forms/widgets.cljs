(ns reagent-forms.widgets
  (:require [reagent.core :as reagent :refer [atom]]))

(defn value-of [element]
 (-> element .-target .-value))

(defn mk-save-fn [doc events]
  (fn [id value]
    (swap! doc
      (fn [current-value]
        (reduce #(or (%2 id value %1) %1) (assoc current-value id value) events)))))

;;coerce the input to the appropriate type
(defmulti format-type
  (fn [widget-type _] widget-type))

(defmethod format-type :numeric
  [_ value]
  (js/parseInt value))

(defmethod format-type :default
  [_ value] value)

;;bind the widget to the document based on its type
(defmulti bind
  (fn [{:keys [widget]} _]
    (if (some #{widget} [:text :numeric :password :email :textarea])
      :input-widget widget)))

(defmethod bind :input-widget
  [{:keys [widget id]} {:keys [get save!]}]
  {:value (get id)
   :on-change #(save! id (->> % (value-of) (format-type widget)))})

(defmethod bind :checkbox
  [{:keys [id]} {:keys [get save! checked]}]
  {:checked @checked
   :on-change #(save! id (swap! checked not))})

(defmethod bind :default [_ _])

(defn set-attrs
  [[type attrs & body] opts & [default-attrs]]
  (into [type (merge default-attrs (bind attrs opts) attrs)] body))

;;initialize the widget by binding it to the document and setting default options
(defmulti init-widget
  (fn [[_ {:keys [widget]}] _]
    (let [widget (keyword widget)]
      (if (some #{widget} [:text :password :email :textarea])
        :input-widget widget))))

(defmethod init-widget :input-widget
  [[_ {:keys [widget]} :as component] opts]
  (fn []
    (set-attrs component opts {:type widget :class "form-control"})))

(defmethod init-widget :checkbox
  [[_ {:keys [id widget]} :as component] {:keys [get] :as opts}]
  (let [state (atom (get id))]
    (fn []
      (set-attrs component (assoc opts :checked state) {:type widget :class "form-control"}))))

(defmethod init-widget :numeric
  [component opts]
  (fn []
    (set-attrs component opts {:type :text :class "form-control"})))

(defmethod init-widget :alert
  [[_ {:keys [id event]} :as component] {:keys [get] :as opts}]
  (fn []
    (when (event (get id))
      (update-in component [1] dissoc :event))))

(defmethod init-widget :radio
  [[type {:keys [widget id value] :as attrs} & body] {:keys [get save!]}]    
  (let [state (atom (= value (get id)))]    
    (fn []
      (into
        [type
         (merge {:type :radio
                 :checked @state
                 :class "form-control"
                 :on-change
                 #(do
                    (save! id value)
                    (reset! state (= value (get id))))}
                attrs)]
         body))))

(defn- group-item [[type {:keys [key] :as attrs} & body] {:keys [save! multi-select]} selections widget id]  
  (letfn [(handle-click! []
           (if multi-select
             (do
               (swap! selections update-in [key] not)
               (save! id (->> @selections (filter second) (map first))))
             (let [value (key @selections)]               
               (reset! selections {key (if value (not value) true)})               
               (save! id (when (key @selections) key)))))] 
    
    (fn []               
      [type (merge {:class (if (key @selections) "active")
                    :on-click handle-click!} attrs) body])))

(defn- mk-selections [id selectors {:keys [get multi-select]}]
  (let [value (get id)]
    (reduce
     (fn [m [_ {:keys [key]}]]
       (assoc m key (boolean (some #{key} (if multi-select value [value])))))
     {} selectors)))

(defn selection-group
  [[type {:keys [widget id] :as attrs} & selection-items] opts]
  (let [selections (atom (mk-selections id selection-items opts))
        selectors (map (fn [item] [(group-item item opts selections widget id)])
                       selection-items)]
    (into [type attrs] selectors)))

(defmethod init-widget :single-select
  [widget opts]
  (selection-group widget opts))

(defmethod init-widget :multi-select
  [widget opts]
  (selection-group widget (assoc opts :multi-select true)))

(defmethod init-widget :list
  [[type {:keys [widget id] :as attrs} & options] {:keys [get save!]}]
  (let [selection (atom (or
                         (get id)
                         (get-in (first options) [1 :key])))]    
    (fn []      
      [type (merge attrs {:on-change #(save! id (value-of %))}) options])))

(defn widget? [node]
  (and (coll? node)
       (map? (second node))
       (contains? (second node) :widget)))

(defn bind-widgets [form doc & events]  
  (let [opts {:get #(get @doc %) :save! (mk-save-fn doc events)}
        form (clojure.walk/prewalk
               (fn [node]
                 (if (widget? node)
                   (let [widget (init-widget node opts)]
                     (if (fn? widget) [widget] widget))
                   node))
               form)]    
    (fn [] form)))
