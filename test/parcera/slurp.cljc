(ns parcera.slurp
  "Some of our tests use `slurp`, which ClojureScript lacks"
  #?(:clj (:refer-clojure :exclude [slurp]))
  #?(:cljs (:require-macros [parcera.test.slurp])))

#?(:clj (defmacro slurp [file]
          (clojure.core/slurp file)))
