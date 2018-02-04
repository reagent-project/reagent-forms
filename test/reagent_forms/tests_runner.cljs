(ns reagent-forms.tests-runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [reagent-forms.core-test]))

(doo-tests 'reagent-forms.core-test)
