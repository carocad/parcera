(ns parcera.antlr.java
  (:require [parcera.antlr.protocols :as antlr])
  (:import (parcera.antlr ClojureParser ClojureLexer)
           (org.antlr.v4.runtime ParserRuleContext Token CommonTokenStream CharStreams ANTLRErrorListener Parser)
           (org.antlr.v4.runtime.tree ErrorNodeImpl)))

(set! *warn-on-reflection* true)


;; A custom Error Listener to avoid Antlr printing the errors on the terminal
;; by default. This is also useful to mimic Instaparse :total parse mechanism
;; such that if we get an error, we can report it as the result instead
(defrecord ParseFailure [reports]
  ANTLRErrorListener
  ;; I am not sure how to use these methods. If you came here wondering why
  ;; is this being printed, please open an issue so that we can all benefit
  ;; from your findings ;)
  (reportAmbiguity [this parser dfa start-index stop-index exact ambig-alts configs]
    ;; TODO
    (println "report ambiguity: " parser dfa start-index stop-index exact ambig-alts configs))
  (reportAttemptingFullContext [this parser dfa start-index stop-index conflicting-alts configs]
    ;; TODO
    (println "report attempting full context: " parser dfa start-index stop-index conflicting-alts configs))
  (reportContextSensitivity [this parser dfa start-index stop-index prediction configs]
    ;; TODO
    (println "report context sensitivity: " parser dfa start-index stop-index prediction configs))
  (syntaxError [this recognizer offending-symbol line char message error]
    ;; recognizer is either clojureParser or clojureLexer
    (let [report (merge {:row     line
                         :column  char
                         :message message
                         :type    :parser}                  ;; todo: lexer should also be allowed
                        (when (instance? Parser recognizer)
                          {:symbol (str offending-symbol)
                           :stack  (->> (.getRuleInvocationStack ^Parser recognizer)
                                        (reverse)
                                        (map keyword))})
                        (when (some? error)
                          {:error error}))]
      (vswap! reports conj report))))


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


(extend-type ClojureParser
  antlr/AntlrParser
  (rules [^ClojureParser this] (vec (.getRuleNames this)))
  (tree [^ClojureParser this] (. this (code))))


(defn parser
  [input]
  (let [chars    (CharStreams/fromString input)
        lexer    (doto (new ClojureLexer chars)
                   (.removeErrorListeners))
        ;; todo: how to handle lexer errors ?
        ;(.addErrorListener listener))
        tokens   (new CommonTokenStream lexer)
        listener (->ParseFailure (volatile! ()))
        parser   (doto (new ClojureParser tokens)
                   (.setBuildParseTree true)
                   (.removeErrorListeners)
                   (.addErrorListener listener))]
    {:parser parser :listener listener}))
