(ns parcera.test.benchmark
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [criterium.core :as criterium]
            [parcera.test.core :as pt]
            [parcera.core :as parcera]))

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


;; execute last ... hopefully
(deftest ^:benchmark z-known-namespace
  (newline)
  (newline)
  (println "Benchmark: Parsing parcera namespace with traces 👮")
  (criterium/quick-bench (parcera/clojure (str '(ns parcera.core
                                                  (:require [instaparse.core :as instaparse]
                                                            [clojure.data :as data]
                                                            [clojure.string :as str]))))
                         :os :runtime :verbose))
