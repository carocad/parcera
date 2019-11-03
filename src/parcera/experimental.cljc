(ns parcera.experimental
  (:import (parcera.antlr clojureParser clojureLexer clojureListener)
           (java.util ArrayList)
           (org.antlr.v4.runtime CharStreams CommonTokenStream ParserRuleContext
                                 Token ANTLRErrorListener Parser)
           (org.antlr.v4.runtime.tree ErrorNode)))


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
                         :message message}
                        (when (instance? Parser recognizer)
                          {:symbol (str offending-symbol)
                           :stack  (->> (.getRuleInvocationStack ^Parser recognizer)
                                        (reverse)
                                        (map keyword))})
                        (when (some? error)
                          {:error error}))]
      (vswap! reports conj report))))


(def default-hidden {:tags     #{:form :collection :literal :keyword :reader_macro :dispatch}
                     :literals #{"(" ")" "[" "]" "{" "}" "#{" "#" "^" "`" "'"
                                 "~@" "@" "#(" "#'" "#_" "#?" "#?@" "##" ":" "::"}})


(defn- info
  "extract the match meta data information from the ast node"
  [^ParserRuleContext ast]
  (let [start (.getStart ast)
        end   (.getStop ast)]
    {::start {:row    (.getLine start)
              :column (.getCharPositionInLine start)}
     ::end   {:row    (.getLine end)
              :column (.getCharPositionInLine end)}}))


(defn- hiccup
  "transform the AST into a `hiccup-like` data structure.

  This function doesnt return a vectors because they are
  100 times slower for this use case compared to `cons` cells"
  [tree rule-names hide-tags hide-literals]
  (cond
    (instance? ParserRuleContext tree)
    (let [rule         (keyword (aget rule-names (.getRuleIndex tree)))
          children-ast (for [child (.-children tree)
                             :let [child-ast (hiccup child rule-names hide-tags hide-literals)]
                             :when (not (nil? child-ast))]
                         child-ast)
          ast          (if (contains? hide-tags rule)
                         (apply concat children-ast)
                         (cons rule children-ast))]
      ;; attach meta data ... ala instaparse
      (with-meta ast (info tree)))

    (instance? ErrorNode tree)
    (let [token (.-symbol tree)
          ;; error metadata
          info  {::start {:row    (.getLine token)
                          :column (.getCharPositionInLine token)}}]
      (with-meta (list ::failure (str tree)) info))

    :else
    (let [text (str tree)]
      (if (contains? hide-literals text) nil text))))


(defn- unhide
  [options]
  (case (:unhide options)
    :all (dissoc default-hidden :literals :tags)
    :content (dissoc default-hidden :literals)
    :tags (dissoc default-hidden :tags)
    default-hidden))


(defn parse
  [input & {:as options}]
  (let [hidden     (unhide options)
        listener   (->ParseFailure (volatile! ()))
        chars      (CharStreams/fromString input)
        lexer      (doto (new clojureLexer chars)
                     (.removeErrorListeners))
        ;; todo: how to handle lexer errors ?
        ;(.addErrorListener listener))
        tokens     (new CommonTokenStream lexer)
        parser     (doto (new clojureParser tokens)
                     (.setBuildParseTree true)
                     (.removeErrorListeners)
                     (.addErrorListener listener))
        rule-names (. parser (getRuleNames))
        tree       (. parser (code))]
    ;(println @(:reports listener))
    (if (or (empty? @(:reports listener)) (:total options))
      (hiccup tree rule-names (:tags hidden) (:literals hidden))
      ;; hide the volatile to avoid exposing mutable memory ;)
      (->ParseFailure @(:reports listener)))))


;(time (parse (slurp "test/parcera/test/core.cljc") :total true))
;(time (parse (slurp "test/parcera/test/core.cljc")))

;(time (parse "(hello @michael \"pink/this will work)" :total true))
;(time (parse "(hello @michael pink/this will work)" :total true))
;(time (parse "(hello @michael \"pink/this will work)"))
;(time (parse "(hello @michael pink/this will work)"))
