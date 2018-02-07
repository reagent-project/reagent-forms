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

(deftest bind-test
  (are [field expected]
       (= (dissoc
           (core/bind field
                      {:get identity})
           :on-change)
          expected)

    {:field :input-field
     :id "12.12312312"
     :fmt "%.2f"}
    {:value "12.12"}

    {:field :checkbox
     :id :id}
    {:checked true}

    {:field :checkbox
     :id false}
    {:checked false}

    {:field :some-field} nil))

(deftest set-attrs-test
  (let [div [:div {:checked true
                   :default-checked true
                   :fmt :fmt
                   :event :event
                   :field :field
                   :inline :inline
                   :save-fn :save-fn
                   :preamble :preamble
                   :postamble :postamble
                   :visible? :visible?
                   :date-format :date-format
                   :auto-close? :auto-close?
                   :random-attr :random-attr}
             "body"]]
    (testing "Attrs are cleaned."
      (is (= (core/set-attrs div {})
             [:div {:random-attr :random-attr} "body"])))
    (testing "Opts are binded."
      (with-redefs [core/bind (fn [attrs opts]
                                (is (= attrs (second div)))
                                opts)]
        (is (= (core/set-attrs div {:get :get :save! :save})
               [:div {:random-attr :random-attr
                      :get :get
                      :save! :save}
                "body"]))))
    (testing "Default attributes are applied."
      (is (= (core/set-attrs div {} {:default-attr :default-attr})
             [:div {:random-attr :random-attr
                    :default-attr :default-attr}
              "body"])))))

(deftest init-field-test
  (let [dissoc-fns (fn [[type attrs & body]]
                     (into [type (dissoc attrs :on-change
                                         :on-blur
                                         :on-focus
                                         :on-blur
                                         :on-change
                                         :on-key-down
                                         :on-mouse-enter
                                         :on-mouse-leave)]
                           body))]

    ; typeahead
    (let [state {:ta "a"}
          [_ input ul]
          ((core/init-field [:div {:field :typeahead
                                   :id :ta
                                   :input-placeholder "pick a friend"
                                   :data-source (fn [])
                                   :input-class "form-control"
                                   :list-class "typeahead-list"
                                   :item-class "typeahead-item"
                                   :highlight-class "highlighted"}]
                            {:doc (atom state)
                             :get (fn [kw] (when kw (kw state)))
                             :save! (fn [& _])
                             :update! (fn [& _])}))]
      (is (= (dissoc-fns input)
             [:input {:placeholder "pick a friend"
                      :disabled nil
                      :value "a"
                      :type :text
                      :class "form-control"}]))
      (is (= (dissoc-fns ul)
             [:ul {:style {:display :none}
                   :class "typeahead-list"}
              []])))

    ; single-select
    (let [state {}
          [_ component]
          ((core/init-field [:div.btn-group {:field :single-select :id :selected}
                             [:button.btn.btn-default {:key :left} "Left"]
                             [:button.btn.btn-default {:key :middle} "Middle"]
                             [:button.btn.btn-default {:key :right} "Right"]]
                            {:doc (atom state)
                             :get (fn [kw] (when kw (kw state)))
                             :save! (fn [& _])
                             :update! (fn [& _])}))]
      (is (= component
             [:div.btn-group {:field :single-select
                              :id :selected}
              [:button.btn.btn-default {:key :left} "Left"]
              [:button.btn.btn-default {:key :middle} "Middle"]
              [:button.btn.btn-default {:key :right} "Right"]])))

    (are [state input expected]
         (let [comp ((core/init-field input {:doc (atom state)
                                             :get (fn [kw] (when kw (kw state)))
                                             :save! (fn [& _])
                                             :update! (fn [[& _]])}))]
           (is (= (dissoc-fns comp) expected)))
      ; container
      {}
      [:div {:field :container
             :valid? :invalid}
       "body"]
      [:div {:valid? :invalid}
       "body"]

      {:id "some-text"}
      [:div {:field :container
             :valid? :id}
       "body"]
      [:div {:valid? :id
             :class "some-text"}
       "body"]

      ; text
      {}
      [:input {:field :text}]
      [:input {:type :text :value ""}]

      {}
      [:input {:field :text :disabled (fn [] false)}]
      [:input {:type :text :value "" :disabled false}]

      {:id "some-text"}
      [:input {:field :text
               :id :id}]
      [:input {:type :text
               :value "some-text"
               :id :id}]

      ; numeric
      {}
      [:input {:field :numeric}]
      [:input {:type :text :value nil}]

      ; checkbox
      {}
      [:input {:field :checkbox :id :non-existent}]
      [:input {:type :checkbox :id :non-existent :checked false}]

      {:id "yep"}
      [:input {:field :checkbox :id :id}]
      [:input {:type :checkbox :id :id :checked true}]

      ; range
      {:id 12}
      [:input {:field :range :min 10 :max 100 :id :id}]
      [:input {:type :range
               :value 12
               :min 10
               :max 100
               :id :id}]

      ; radio
      {}
      [:input {:field :radio :value :b :name :radio}]
      [:input {:name :radio :type :radio :checked false}]

      {:id :a}
      [:input {:field :radio :value :a :name :id}]
      [:input {:name :id :type :radio :checked true}]

      ; file
      {}
      [:input {:field :file :type :file}]
      [:input {:type :file}]

      {}
      [:input {:field :file :multiple true}]
      [:input {:type :file :multiple true}]

      ; list
      {}
      [:select {:field :list :id :many-options}
       [:option {:key :foo} "foo"]
       [:option {:key :bar} "bar"]
       [:option {:key :baz} "baz"]]
      [:select {:id :many-options :default-value "foo"}
       [[:option {:key :foo} "foo"]
        [:option {:key :bar} "bar"]
        [:option {:key :baz} "baz"]]]

      {:many-options :bar}
      [:select {:field :list :id :many-options}
       [:option {:key :foo} "foo"]
       [:option {:key :bar} "bar"]
       [:option {:key :baz} "baz"]]
      [:select {:id :many-options :default-value "bar"}
       [[:option {:key :foo} "foo"]
        [:option {:key :bar} "bar"]
        [:option {:key :baz} "baz"]]])))

(deftest bind-fields-test
  (with-redefs [core/wrap-fns (fn [_ node] node)
                core/init-field (fn [node _] node)]
    (let [component [:div
                     [:input {:field :text :id :a}]
                     [:input {:field :numeric}]
                     [:input {:field :range}]]
          result ((core/bind-fields component nil))]
      (is (= result component)))))
