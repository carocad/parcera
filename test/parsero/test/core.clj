(ns parsero.test.core
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check :as tc]
            [parsero.core :as parsero]))

(def roundtrip
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= input (parsero/code (parsero/clojure input)))))

(tc/quick-check 100 roundtrip)

(parsero/code (parsero/clojure (pr-str #uuid "cac6587a-df79-4faf-9ada-0ddd0cd261a4")))
