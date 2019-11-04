(ns parcera.antlr.protocols
  "These protocols are a cheat: I use them to be able to dispatch
  to both Java and JavaScript parser implementations without the
  common code having to know about it"
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
