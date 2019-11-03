(ns parcera.antlr.java
  (:require [parcera.antlr.protocols :as antlr])
  (:import (parcera.antlr clojureParser clojureLexer)
           (org.antlr.v4.runtime ParserRuleContext Token CommonTokenStream CharStreams)
           (org.antlr.v4.runtime.tree ErrorNodeImpl)))

(set! *warn-on-reflection* true)


(extend-type ParserRuleContext
  antlr/ParserRule
  (children [^ParserRuleContext this] (.-children this))
  (rule-index [^ParserRuleContext this] (.getRuleIndex this))
  (start [^ParserRuleContext this] (.getStart this))
  (end [^ParserRuleContext this] (.getStop this)))


(extend-type ErrorNodeImpl
  antlr/ErrorNode
  (token [^ErrorNodeImpl this] (.-symbol this)))


(extend-type Token
  antlr/Token
  (row [^Token this] (.getLine this))
  (column [^Token this] (.getCharPositionInLine this)))


(extend-type clojureParser
  antlr/AntlrParser
  (rules [^clojureParser this] (vec (.getRuleNames this)))
  (tree [^clojureParser this] (. this (code))))

(defn parser
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
