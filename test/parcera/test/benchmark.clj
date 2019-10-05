(ns parcera.test.benchmark
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [criterium.core :as criterium]
            [parcera.test.core :as pt]))

(deftest ^:benchmark parsing
  (println "Benchmark: Time parsing Clojure values ⌛")
  (criterium/quick-bench (tc/quick-check 30 pt/validity)
                         :os :runtime :verbose))

(deftest ^:benchmark roundtrip
  (newline)
  (newline)
  (println "Benchmark: Round trip of Clojure values 🚀")
  (criterium/quick-bench (tc/quick-check 30 pt/symmetric)
                         :os :runtime :verbose))
