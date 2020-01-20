(ns parcera.benchmark
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [criterium.core :as criterium]
            [parcera.test.core :as pt]
            [parcera.core :as parcera]))

(deftest ^:benchmark parsing
  (newline)
  (newline)
  (println "Benchmark: Parsing automatically generated values")
  (criterium/quick-bench (tc/quick-check 30 pt/validity)
                         :os :runtime :verbose)
  (newline)
  (newline)
  (println "Benchmark: Round trip of automatically generated values")
  (criterium/quick-bench (tc/quick-check 30 pt/symmetric)
                         :os :runtime :verbose))


(deftest ^:benchmark clojure.core-roundtrip
  (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojure/master/src/clj/clojure/core.clj")]
    (newline)
    (newline)
    (println "Benchmark: Parsing Clojure's core namespace 🚧")
    (criterium/quick-bench (parcera/ast core-content :optimize :memory)
                           :os :runtime :verbose)
    (newline)
    (newline)
    (println "Benchmark: Rountrip Clojure's core namespace 🚧")
    (criterium/quick-bench (parcera/code (parcera/ast core-content :optimize :memory))
                           :os :runtime :verbose)))
