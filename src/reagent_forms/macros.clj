(ns reagent-forms.macros)

(defmacro render-element [attrs doc & body]
  `(fn []
     (let [disabled-path# (case (:field ~attrs)
                           :typeahead [1 1 :disabled]
                           :datepicker [1 1 1 :disabled]
                           [1 :disabled])
           body# (if (and (:disabled ~attrs)
                          (get-in ~@body disabled-path#))
                   (update-in ~@body disabled-path# #(if (fn? %) (%) %))
                   ~@body)]
       (if-let [visible# (:visible? ~attrs)]
         (when (visible# (deref ~doc))
           body#)
         body#))))
