(ns reagent-forms.macros)

(defmacro render-element [attrs doc & body]
  `(fn []
     (let [body# (if (:disabled ~attrs)
                   (update-in ~@body [1 :disabled] #(if (fn? %) (%) %))
                   ~@body)]
       (if-let [visible# (:visible? ~attrs)]
         (when (visible# (deref ~doc))
           body#)
         body#))))
