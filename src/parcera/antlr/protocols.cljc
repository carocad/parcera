(ns parcera.antlr.protocols
  (:import (org.antlr.v4.runtime ANTLRErrorListener Parser)))


(defprotocol AntlrParser
  (rules [this])
  (tree [this]))

(defprotocol ParserRule
  (children [this])
  (rule-index [this])
  (start [this])
  (end [this]))


(defprotocol Token
  (row [this])
  (column [this]))

(defprotocol ErrorNode
  (token [this]))
