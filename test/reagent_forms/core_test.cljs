(ns reagent-forms.core-test
  (:require [clojure.test :refer [deftest is are testing]]
            [reagent.core :as r]
            [reagent-forms.core :as core]))

(defn- update&double
  [path value doc]
  (update-in doc path * 2))

(deftest scroll-to-test
  (let [item-elem {:scrollHeight 100
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
             (assoc-in doc path value))]
    (is (= (core/run-events state :kw 2 [f1 update&double update&double])
           {:kw 8}))))

(deftest mk-update-fn-test
  (testing "Value is associated in the doc."
    (let [state (r/atom {:kw 5})
          f (core/mk-update-fn state [])
          update-fn (fn [_ val] val)]
      (f :kw update-fn :val)
      (is (= @state {:kw :val}))))
  (testing "Returned function runs all the events."
    (let [state (r/atom {:kw 5})
          f (core/mk-update-fn state [update&double update&double])
          update-fn (fn [_ val] val)]
      (f :kw update-fn 10)
      (is (= @state {:kw 40})))))

(deftest mk-save-fn-test
  (testing "Value is associated in the doc."
    (let [state (r/atom {})
          f (core/mk-save-fn state [])]
      (f :kw :val)
      (is (= @state
             {:kw :val}))))
  (testing "Returned function runs all the events."
    (let [state (r/atom {})
          f (core/mk-save-fn state [update&double update&double])]
      (f :kw 1)
      (is (= @state {:kw 4})))))

(deftest wrap-fns-test
  (testing "Functions map is properly formed."
    (let [fns {:doc :doc-fn
               :get :get-fn
               :save! :save-fn
               :update! :update-fn}]
      (is (= (core/wrap-fns fns nil)
             {:doc :doc-fn
              :get :get-fn
              :save! :save-fn
              :update! :update-fn}))))
  (testing "Functions are being wrapped."
    (let [fns {:doc :doc-fn
               :get :get-fn
               :save! :save-fn
               :update! :update-fn}
          node [:div {:in-fn :in-fn
                      :out-fn :out-fn}]
          mock-wrap-fn (partial conj [])]
      (with-redefs [core/wrap-get-fn mock-wrap-fn
                    core/wrap-save-fn mock-wrap-fn
                    core/wrap-update-fn mock-wrap-fn]
        (is (= (core/wrap-fns fns node)
               {:doc :doc-fn
                :get [:get-fn :in-fn]
                :save! [:save-fn :out-fn]
                :update! [:update-fn :out-fn]}))))))

(deftest format-value-test
  (are [format input expected]
       (= (core/format-value format input) expected)
    "%.2f" "0.123123" "0.12"
    "%d" "3.123123" "3"))

(deftest format-type-test
  (are [field-type input expected]
       (= (core/format-type field-type input) expected)
    :numeric nil nil
    :numeric "xyz" nil
    :numeric "12" 12
    :numeric "12xyz" 12
    :numeric "0.123" 0.123

    :range nil nil
    :range "xyz" nil
    :range "12" 12
    :range "12xyz" 12
    :range "0.123" 0.123

    :other-type nil nil
    :other-type "xyz" "xyz"
    :other-type "12" "12"))
