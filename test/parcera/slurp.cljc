(ns parcera.slurp
  "Some glue to help with CLJC things"
  #?(:clj (:refer-clojure :exclude [slurp])))

#?(:clj (defmacro slurp [file]
          (clojure.core/slurp file)))