(ns reagent-forms.macros
 (:require [clojure.walk :refer [postwalk]]))

(defmacro render-element [attrs doc & body]
  `(fn []
     (let [update-disabled?# (not (some #{(:field ~attrs)}
                                        [:multi-select :single-select]))
           body# (postwalk
                   (fn [c#]
                     (if (and (map? c#)
                              (not (nil? (:disabled c#)))
                              update-disabled?#)
                       (update c# :disabled #(if (fn? %) (%) %))
                       c#))
                   ~@body)]
       (if-let [visible# (:visible? ~attrs)]
         (let [pred# (if (fn? visible#)
                       (visible# (deref ~doc))
                       (~doc visible#))]
          (when pred# body#))
         body#))))
