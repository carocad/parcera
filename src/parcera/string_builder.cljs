(ns parcera.string-builder
  "Home for our JavaScript StringBuilder")

(defn StringBuilder
  "A JavaScript StringBuilder with just the interface parcera needs"
  []
  (let [store (js/Array.)]
    (reify
      Object
      (append [this s] (.push store s))
      (toString [this s] (.join store "")))))