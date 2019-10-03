(ns parsero.test.benchmark
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [criterium.core :as c]
            [parsero.test.core :as pt]))

(deftest ^:benchmark parsing
  (println "Benchmark: Time parsing Clojure values ⌛")
  (c/quick-bench (tc/quick-check 100 pt/validity)
                 :os :runtime :verbose))

(deftest ^:benchmark roundtrip
  (println "Benchmark: Round trip of Clojure values 🚀")
  (c/quick-bench (tc/quick-check 100 pt/symmetric)
                 :os :runtime :verbose))
