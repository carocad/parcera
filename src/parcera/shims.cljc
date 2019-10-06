(ns parcera.shims
  "Some glue to help with CLJC things"
  #?(:clj (:refer-clojure :exclude [slurp])))

#?(:clj (defmacro slurp [file]
          (clojure.core/slurp file)))

#?(:cljs (defn StringBuilder
           "A JavaScript StringBuilder with just the interface parcera needs"
           []
           (let [store (js/Array.)]
             (reify
               Object
               (append [this s] (.push store s))
               (toString [this s] (.join store ""))))))