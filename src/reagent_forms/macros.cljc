(ns reagent-forms.macros
  (:require [clojure.walk :refer [postwalk]]))

(defmacro render-element [attrs doc & body]
  `(fn []
     (let [update-disabled?# (not (some #{(:field ~attrs)}
                                        [:multi-select :single-select]))
           body#             (postwalk
                               (fn [c#]
                                 (if (map? c#)
                                   (-> c#
                                       (reagent-forms.core/set-validation-class ~doc)
                                       (reagent-forms.core/update-attrs ~doc)
                                       (reagent-forms.core/set-disabled update-disabled?#)
                                       (reagent-forms.core/clean-attrs))
                                   c#))
                               ~@body)]
       (if-let [visible# (:visible? ~attrs)]
         (when (reagent-forms.core/call-attr ~doc visible#)
           body#)
         body#))))
