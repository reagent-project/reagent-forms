(ns reagent-forms.macros)

(defmacro render-element [attrs doc & body]
  `(fn []
     (if-let [visible# (:visible? ~attrs)]
       (when (visible# (deref ~doc))
         ~@body)
       ~@body)))
