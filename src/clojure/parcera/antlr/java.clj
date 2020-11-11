(ns parcera.antlr.java
  "mapping functions between Antlr's Java implementation
  and Parcera AST representation"
  (:require [parcera.antlr.common :as common]
            [clojure.core.protocols :as clojure])
  (:import (parcera.antlr ClojureParser ClojureLexer)
           (org.antlr.v4.runtime ParserRuleContext CommonTokenStream
                                 CharStreams ANTLRErrorListener Parser)
           (org.antlr.v4.runtime.tree ErrorNodeImpl TerminalNode)))


(set! *warn-on-reflection* true)


;; A custom Error Listener to avoid Antlr printing the errors on the terminal
;; by default. This is also useful to mimic Instaparse :total parse mechanism
;; such that if we get an error, we can report it as the result instead.
;; Besides `syntaxError` all other methods are overridden only to get ambiguity
;; reports during development. If this is somehow shown on your application
;; please report it :)
(defrecord AntlrFailure [reports]
  ANTLRErrorListener
  (reportAmbiguity [this parser dfa start-index stop-index exact ambig-alts configs]
    (println "report ambiguity: " parser dfa start-index stop-index exact ambig-alts configs))
  (reportAttemptingFullContext [this parser dfa start-index stop-index conflicting-alts configs]
    (println "report attempting full context: " parser dfa start-index stop-index conflicting-alts configs))
  (reportContextSensitivity [this parser dfa start-index stop-index prediction configs]
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


(defn- parser-rule-meta
  [^ParserRuleContext this]
  (let [start (.getStart this)
        stop  (.getStop this)]
    (if (nil? stop)
      ;; no end found - happens on errors
      {:parcera.core/start {:row    (.getLine start)
                            :column (.getCharPositionInLine start)}}

      {:parcera.core/start {:row    (.getLine start)
                            :column (.getCharPositionInLine start)}
       :parcera.core/end   {:row    (.getLine stop)
                            :column (Math/addExact (.getCharPositionInLine stop)
                                                   (.length (.getText stop)))}})))

;; start and end are tokens not positions.
;; So '(hello/world)' has '(' 'hello/world' and ')' as tokens
(extend-type ParserRuleContext
  common/Antlr
  (ast [this rule-names hide-tags hide-literals]
    (let [meta     (parser-rule-meta this)
          rule     (get rule-names (.getRuleIndex this))
          children (sequence (comp (map #(common/ast % rule-names hide-tags hide-literals))
                                   (remove nil?))
                             (.-children this))]
      (if (contains? hide-tags rule)
        ;; parcera hidden tags are always "or" statements, so just take the single children
        (first children)
        ;; attach meta data ... ala instaparse
        (with-meta (cons rule children) meta)))))


(extend-type ErrorNodeImpl
  common/Antlr
  (ast [this rule-names hide-tags hide-literals]
    (let [token (.-symbol this)]
      (with-meta (list :parcera.core/failure (:content (str this)))
                 {:parcera.core/start {:row    (.getLine token)
                                       :column (.getCharPositionInLine token)}}))))


(extend-type TerminalNode
  common/Antlr
  (ast [this rule-names hide-tags hide-literals]
    (let [content (str this)]
      (when-not (contains? hide-literals content)
        content))))


(defn ast
  "utility function to allow java and javascript to interoperate"
  [this rule-names hide-tags hide-literals]
  (common/ast this rule-names hide-tags hide-literals))


(defn parse
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
    {:rules   (into [] (map keyword) (.getRuleNames parser))
     :tree    (.code parser)
     :reports @(:reports listener)}))
