(ns parcera.experimental
  (:import (parcera.antlr clojureParser clojureLexer clojureListener)
           (java.util ArrayList)
           (org.antlr.v4.runtime CharStreams CommonTokenStream ParserRuleContext)))

;; todo: add metadata to each node
(defn- hiccup
  [ast rule-names]
  (if (and (instance? ParserRuleContext ast) (not-empty (.-children ast)))
    (into [(keyword (aget rule-names (.getRuleIndex ast)))]
          (for [child (.-children ast)]
            (hiccup child rule-names)))
    (.toString ast)))


(let [input      "(john :SHOUTS \"hello\" @michael pink/this will work)"
      chars      (CharStreams/fromString input)
      lexer      (new clojureLexer chars)
      tokens     (new CommonTokenStream lexer)
      parser     (new clojureParser tokens)
      rule-names (. parser (getRuleNames))
      _          (. parser (setBuildParseTree true))
      tree       (. parser (code))]
  (hiccup tree rule-names))

