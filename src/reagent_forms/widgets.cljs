(ns reagent-forms.widgets
  (:require [reagent.core :as reagent :refer [atom]]))

(defn value-of [element]
 (-> element .-target .-value))

(defn get-value [doc id]
  (get @doc id))

(defn set-value! [doc id value]
  (swap! doc assoc id value))

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
  (fn [widget _ _]
    widget))

(defn bind-input [widget id doc]
  {:value (get-value doc id)
   :on-change #(set-value! doc id (->> % (value-of) (format-type widget)))})

(defmethod bind :text [widget id doc]
  (bind-input widget id doc))

(defmethod bind :numeric [widget id doc]
  (bind-input widget id doc))

(defmethod bind :password [widget id doc]
  (bind-input widget id doc))

(defmethod bind :email [widget id doc]
  (bind-input widget id doc))

(defmethod bind :textarea [widget id doc]
  (bind-input widget id doc))

(defmethod bind :checkbox [widget id doc]
  {:value (get-value doc id)
   :on-change #(set-value! doc id (.-checked (.-target %)))})

(defmethod bind :default [_ _ _])

(defn set-opts
  [[type {:keys [id widget] :as opts} & body] doc schema & [default-opts]]  
  (into
   [type
    (merge default-opts (id schema) (bind widget id doc) opts)]
   body))

;;initialize the widget by binding it to the document and setting default options
(defmulti init-widget
  (fn [[_ {:keys [widget]}] _ _]
    (keyword widget)))

(defmethod init-widget :text
  [widget doc schema]
  (set-opts widget doc schema {:type :text :class "form-control"}))

(defmethod init-widget :numeric
  [widget doc schema]
  (set-opts widget doc schema {:type :text :class "form-control"}))

(defmethod init-widget :password
  [widget doc schema]
  (set-opts widget doc schema {:type :password :class "form-control"}))

(defmethod init-widget :email
  [widget doc schema]
  (set-opts widget doc schema {:type :email :class "form-control"}))

(defmethod init-widget :textarea
  [widget doc schema]
  (set-opts widget doc schema {:class "form-control"}))

(defmethod init-widget :checkbox
  [widget doc schema]
  (set-opts widget doc schema {:type :checkbox :class "form-control"}))

(defmethod init-widget :radio
  [[type {:keys [id widget value] :as opts} & body] doc schema]    
  (into
    [type
     (merge (id schema)
            {:type :radio
             :class "form-control"
             :on-change #(set-value! doc id value)}
            opts)]
     body))
  

(defn- group-item [selections doc widget id multi? [type {:keys [key] :as opts} & body]]
  (letfn [(handle-click! []
           (if multi?
             (do
               (swap! selections update-in [key] not)
               (set-value! doc id (->> @selections (filter second) (map first))))
             (do
               (reset! selections {key (not (key @selections))})                              
               (set-value! doc id (when (key @selections) key)))))] 
    
    (fn []               
      [type (merge {:class (if (key @selections) "active")
                    :on-click handle-click!} opts) body])))

(defn- mk-selections [selectors doc-values]
  (->> selectors
       (map (fn [[_ {:keys [key]}]] [key (boolean (some #{key} doc-values))]))
       (into {}) atom))

(defn selection-group
  [[type {:keys [widget id] :as opts} & selection-items] doc schema multi?]
  (let [selections (mk-selections selection-items (get-value doc id))
        selectors (map (fn [item] [(group-item selections doc widget id multi? item)])
                       selection-items)]
    (into [type opts] selectors)))

(defmethod init-widget :single-select
  [widget doc schema]
  (selection-group widget doc schema false))

(defmethod init-widget :multi-select
  [widget doc schema]
  (selection-group widget doc schema true))

(defmethod init-widget :list
  [[type {:keys [widget id] :as opts} & options] doc schema]
  (let [selection (atom (or
                         (get-value doc id)
                         (get-in (first options) [1 :key])))]    
    (fn []      
      [type (merge opts {:on-change #(set-value! doc id (value-of %))}) options])))

(defn widget? [node]
  (and (coll? node)
       (map? (second node))
       (contains? (second node) :widget)))

(defn bind-widgets [form schema doc]  
  (let [form (clojure.walk/prewalk
               (fn [node]
                 (if (widget? node)
                   (let [widget (init-widget node doc schema)]
                     (if (fn? widget) [widget] widget))
                   node))
               form)]    
    (fn [] form)))
