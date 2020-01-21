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
  clojure/Datafiable
  (datafy [this]
    (common/->Node (parser-rule-meta this)
                   :parcera.core/rule
                   (.getRuleIndex this)
                   (.-children this))))


(extend-type ErrorNodeImpl
  clojure/Datafiable
  (datafy [this]
    (let [token (.-symbol this)]
      (common/->Node {:parcera.core/start {:row    (.getLine token)
                                           :column (.getCharPositionInLine token)}}
                     :parcera.core/failure
                     nil                                    ; rule id
                     (str this)))))                         ; content


(extend-type TerminalNode
  clojure/Datafiable
  (datafy [this] (common/->Node nil                         ; meta
                                :parcera.core/terminal      ;type
                                nil                         ; rule id
                                (str this))))               ; content


;; just an utility to allow js to use the same code
(defn datafy [tree] (clojure/datafy tree))

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
