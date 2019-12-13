(ns parcera.antlr.javascript
  (:require [parcera.antlr.common :as common]
            [antlr4 :refer [CharStreams CommonTokenStream]]
            [parcera.antlr.ClojureLexer :refer [ClojureLexer]]
            [parcera.antlr.ClojureParser :refer [ClojureParser]]))

(set! *warn-on-infer* true)


#_(extend-type ParserRuleContext
    antlr/ParserRule
    (children [^ParserRuleContext this] (.-children this))
    (rule-index [^ParserRuleContext this] (.getRuleIndex this))
    (start [^ParserRuleContext this] (.getStart this))
    (end [^ParserRuleContext this] (.getStop this)))


#_(extend-type ErrorNodeImpl
    antlr/ErrorNode
    (token [^ErrorNodeImpl this] (.-symbol this)))


#_(extend-type Token
    antlr/Token
    (row [^Token this] (.getLine this))
    (column [^Token this] (.getCharPositionInLine this)))


#_(extend-type clojureParser
    antlr/AntlrParser
    (rules [^clojureParser this] (vec (.getRuleNames this)))
    (tree [^clojureParser this] (. this (code))))


(defn parse
  [input]
  {:tree input})

#_(defn parser
    [input listener]
    (let [chars  (CharStreams/fromString input)
          lexer  (doto (new clojureLexer chars)
                   (.removeErrorListeners))
          ;; todo: how to handle lexer errors ?
          ;(.addErrorListener listener))
          tokens (new CommonTokenStream lexer)]
      (doto (new clojureParser tokens)
        (.setBuildParseTree true)
        (.removeErrorListeners)
        (.addErrorListener listener))))
