(ns parcera.antlr.java
  (:require [parcera.antlr.protocols :as antlr])
  (:import (parcera.antlr ClojureParser ClojureLexer)
           (org.antlr.v4.runtime ParserRuleContext Token CommonTokenStream CharStreams ANTLRErrorListener Parser)
           (org.antlr.v4.runtime.tree ErrorNodeImpl)))

(set! *warn-on-reflection* true)


;; A custom Error Listener to avoid Antlr printing the errors on the terminal
;; by default. This is also useful to mimic Instaparse :total parse mechanism
;; such that if we get an error, we can report it as the result instead
(defrecord AntlrFailure [reports]
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
                         :type    (if (instance? Parser recognizer) :parser :lexer)}
                        (when (instance? Parser recognizer)
                          {:symbol (str offending-symbol)
                           :stack  (->> (.getRuleInvocationStack ^Parser recognizer)
                                        (reverse)
                                        (map keyword))})
                        (when (some? error)
                          {:error error}))]
      (vswap! reports conj report))))


;; start and end are tokens not positions.
;; So '(hello/world)' has '(' 'hello/world' and ')' as tokens
(extend-type ParserRuleContext
  antlr/ParserRule
  (children [^ParserRuleContext this] (.-children this))
  (rule-index [^ParserRuleContext this] (.getRuleIndex this))
  antlr/LocationInfo
  (span [^ParserRuleContext this]
    (let [start (.getStart this)
          stop  (.getStop this)]
      (cond
        ;; happens when the parser rule is a single lexer rule
        (identical? start stop)
        {:parcera.core/start {:row    (.getLine start)
                              :column (.getCharPositionInLine start)}
         :parcera.core/end   {:row    (.getLine start)
                              :column (Math/addExact (.getCharPositionInLine start)
                                                     (.length (.getText start)))}}

        ;; no end found - happens on errors
        (nil? stop)
        {:parcera.core/start {:row    (.getLine start)
                              :column (.getCharPositionInLine start)}}

        :else
        {:parcera.core/start {:row    (.getLine start)
                              :column (.getCharPositionInLine start)}
         :parcera.core/end   {:row    (.getLine stop)
                              :column (Math/addExact (.getCharPositionInLine stop)
                                                     (.length (.getText stop)))}}))))


(extend-type ErrorNodeImpl
  antlr/LocationInfo
  (span [^ErrorNodeImpl this]
    (let [token (.-symbol this)]
      {:parcera.core/start {:row    (.getLine token)
                            :column (.getCharPositionInLine token)}})))


(extend-type ClojureParser
  antlr/AntlrParser
  (rules [^ClojureParser this] (into [] (map keyword) (.getRuleNames this)))
  (tree [^ClojureParser this] (. this (code))))


(defn parser
  [input]
  (let [listener (->AntlrFailure (volatile! ()))
        chars    (CharStreams/fromString input)
        lexer    (doto (new ClojureLexer chars)
                   (.removeErrorListeners)
                   (.addErrorListener listener))
        tokens   (new CommonTokenStream lexer)
        parser   (doto (new ClojureParser tokens)
                   (.setBuildParseTree true)
                   (.removeErrorListeners)
                   (.addErrorListener listener))]
    {:parser parser :errors {:parser listener}}))
