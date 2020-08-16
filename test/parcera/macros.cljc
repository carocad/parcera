(ns parcera.macros
  "Some of our tests use `slurp`, which ClojureScript lacks"
  #?(:clj (:refer-clojure :exclude [slurp]))
  #?(:cljs (:require-macros [parcera.macros]))
  (:require [clojure.test :refer [is]]
            [parcera.core :as parcera]))

#?(:clj (defmacro slurp* [file]
          (clojure.core/slurp file)))

#?(:clj (defmacro roundtrip
          "checks parcera can parse and write back the exact same input code"
          [input]
          `(is (= ~input (parcera/code (parcera/ast ~input))))))


#?(:clj (defmacro valid?
          [input]
          `(is (not (parcera/failure? (parcera/ast ~input))))))
