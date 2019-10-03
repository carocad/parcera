(ns parsero.test.core
  (:require [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check :as tc]
            [parsero.core :as parsero]
            [instaparse.core :as instaparse]))

(def symmetric
  "The read <-> write process of parsero MUST be symmetrical. Meaning
  that the AST and the text representation are equivalent"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= input (parsero/code (parsero/clojure input)))))


(def little-ambiguity
  "The process of parsing clojure code yields consistent results. Meaning
  that any input should (but must not) only have 1 AST representation ... however
  I have found this is not always possible"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (>= 2 (count (instaparse/parses parsero/clojure input)))))


(deftest parsero

  (testing "clojure values"
    (let [result (tc/quick-check 100 symmetric)]
      (is (:pass? result)
          (str "read <-> write process yield different result. Failed at\n"
               (with-out-str (pprint/pprint result))))))

  (testing "very little ambiguity"
    (let [result (tc/quick-check 100 little-ambiguity)]
      (is (and (:pass? result))
          (str "high ambiguity case found. Please check the grammar to ensure "
               "high accuracy\n"
               (with-out-str (pprint/pprint result)))))))


;(instaparse/parses parsero/clojure "[0N 0N]")
