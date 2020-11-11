(ns parcera.antlr.common)

(defrecord Node [metadata type rule-id content])

(defprotocol Antlr
  (ast [this rule-names hide-tags hide-literals]))
