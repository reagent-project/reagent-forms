(ns reagent-forms.widgets
  (:require [reagent.core :as reagent :refer [atom]]))

(defn value-of [element]
 (-> element .-target .-value))

(defn get-value [doc id]
  (get @doc id))

(defn mk-save-fn [listeners]
  (fn [doc id value]
    (swap! doc assoc id value)
    (doseq [listener (id listeners)]
      (listener doc id value))))

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
  [{:keys [widget id]} {:keys [doc save!]}]
  {:value (get-value doc id)
   :on-change #(save! doc id (->> % (value-of) (format-type widget)))})

(defmethod bind :checkbox
  [{:keys [id]} {:keys [doc save!]}]
  {:value (get-value doc id)
   :on-change #(save! doc id (.-checked (.-target %)))})

(defmethod bind :default [_ _ _])

(defn set-attrs
  [[type attrs & body] opts & [default-attrs]]
  (into [type (merge default-attrs (bind attrs opts) attrs)] body))

;;initialize the widget by binding it to the document and setting default options
(defmulti init-widget
  (fn [[_ {:keys [widget]}] _]
    (let [widget (keyword widget)]
      (if (some #{widget} [:text :password :email :textarea :checkbox])
        :input-widget widget))))

(defmethod init-widget :input-widget
  [[_ {:keys [widget]} :as component] opts]
  (set-attrs component opts {:type widget :class "form-control"}))

(defmethod init-widget :numeric
  [component opts]
  (set-attrs component opts {:type :text :class "form-control"}))

(defmethod init-widget :radio
  [[type {:keys [id widget value] :as attrs} & body] {:keys [doc save!]}]    
  (into
    [type
     (merge {:type :radio
             :class "form-control"
             :on-change #(save! doc id value)}
            attrs)]
     body))

(defn- group-item [[type {:keys [key] :as attrs} & body] {:keys [doc save! multi-select]} selections widget id]
  (letfn [(handle-click! []
           (if multi-select
             (do
               (swap! selections update-in [key] not)
               (save! doc id (->> @selections (filter second) (map first))))
             (do
               (reset! selections {key (not (key @selections))})                              
               (save! doc id (when (key @selections) key)))))] 
    
    (fn []               
      [type (merge {:class (if (key @selections) "active")
                    :on-click handle-click!} attrs) body])))

(defn- mk-selections [selectors doc-values]
  (->> selectors
       (map (fn [[_ {:keys [key]}]] [key (boolean (some #{key} doc-values))]))
       (into {}) atom))

(defn selection-group
  [[type {:keys [widget id] :as attrs} & selection-items] opts]
  (let [selections (mk-selections selection-items (get-value (:doc opts) id))
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
  [[type {:keys [widget id] :as attrs} & options] {:keys [doc save!]}]
  (let [selection (atom (or
                         (get-value doc id)
                         (get-in (first options) [1 :key])))]    
    (fn []      
      [type (merge attrs {:on-change #(save! doc id (value-of %))}) options])))

(defn widget? [node]
  (and (coll? node)
       (map? (second node))
       (contains? (second node) :widget)))

(defn bind-widgets [form doc & [listeners]]  
  (let [opts {:doc doc :save! (mk-save-fn listeners)}
        form (clojure.walk/prewalk
               (fn [node]
                 (if (widget? node)
                   (let [widget (init-widget node opts)]
                     (if (fn? widget) [widget] widget))
                   node))
               form)]    
    (fn [] form)))
