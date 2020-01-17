(ns parcera.antlr.javascript
  (:require [parcera.antlr.common :as common]
            [reader :as reader]))

;(common/map->Node {})

;(set! *warn-on-infer* true)

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
  (let [chars  (reader/charStreams input)
        lexer  (doto (reader/lexer chars)
                 (.removeErrorListeners))
        ;(.addErrorListener listener))
        tokens (reader/tokens lexer)]
    (doto (reader/parser tokens)
      ;(.setBuildParseTree true)
      (.removeErrorListeners))))
;(.addErrorListener listener))))


;(.getText (.code (parse "hello/world")))
