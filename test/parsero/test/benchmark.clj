(ns parsero.test.benchmark
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [criterium.core :as criterium]
            [parsero.test.core :as pt]))

(deftest ^:benchmark parsing
  (println "Benchmark: Time parsing Clojure values ⌛")
  (criterium/quick-bench (tc/quick-check 30 pt/validity)
                         :os :runtime :verbose))

(newline)
(newline)

(deftest ^:benchmark roundtrip
  (println "Benchmark: Round trip of Clojure values 🚀")
  (criterium/quick-bench (tc/quick-check 30 pt/symmetric)
                         :os :runtime :verbose))
