(ns reagent-forms.core-test
  (:require [clojure.test :refer [deftest is are testing]]
            [reagent.core :as r]
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

(deftest id->path-test
  (are [input expected]
       (= (core/id->path input) expected)
    :a [:a]
    :a.b.c [:a :b :c]))

(deftest cursor-for-id-test
  (with-redefs [reagent.core/cursor (fn [doc id] [doc id])]
    (is (= (core/cursor-for-id :doc :a.b.c)
           [:doc [:a :b :c]]))))

(deftest run-events-test
  (let [state {}
        f1 (fn [path value doc]
             (assoc-in doc path value))
        f2 (fn [path value doc]
             (update-in doc path * 2))
        f3 (fn [path value doc]
             (update-in doc path * 2))]
    (is (= (core/run-events state :kw 2 [f1 f2 f3])
           {:kw 8}))))

(deftest mk-update-fn-test
  (testing "Value is associated in the doc"
    (let [state (r/atom {:kw 5})
          f (core/mk-update-fn state [])
          update-fn (fn [_ val] val)]
     (f :kw update-fn 10)
     (is (= @state {:kw 10}))))
  (testing "Returned function runs all the events."
    (let [state (r/atom {:kw 5})
          f1 (fn [path value doc]
               (update-in doc path * 2))
          f2 (fn [path value doc]
               (update-in doc path * 2))
          f (core/mk-update-fn state [f1 f2])
          update-fn (fn [_ val] val)]
     (f :kw update-fn 10)
     (is (= @state {:kw 40})))))
