(ns reagent-forms.core-test
  (:require [clojure.test :refer [deftest is are]]
            [reagent-forms.core :as core]))

(deftest scroll-to-test
  (let [f #'core/scroll-to
        item-elem {:scrollHeight 100
                   :offsetTop 300}
        list-elem (fn [i]
                    (is (= i 0))
                    (clj->js {:scrollTop 999
                              :children
                              {:item (fn [idx]
                                       (is (= idx 3))
                                       (clj->js item-elem))}}))
        ul (fn [tag-name]
             (is (= tag-name "ul"))
             (clj->js {:item list-elem}))
        element (clj->js
                 {:target
                  {:parentNode
                   {:getElementsByTagName ul}}})]
      (is (= 100 (core/scroll-to element 3)))))
