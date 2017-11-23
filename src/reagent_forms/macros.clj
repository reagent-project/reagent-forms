(ns reagent-forms.macros)

(defmacro render-element [attrs doc & body]
  `(fn []
     (let [disabled-path# (if (= :typeahead (:field ~attrs))
                           [1 1 :disabled]
                           [1 :disabled])
           body# (if (:disabled ~attrs)
                   (update-in ~@body disabled-path# #(if (fn? %) (%) %))
                   ~@body)]
       (if-let [visible# (:visible? ~attrs)]
         (when (visible# (deref ~doc))
           body#)
         body#))))
