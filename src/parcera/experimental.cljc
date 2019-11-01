(ns parcera.experimental
  (:import (parcera.antlr clojureParser clojureLexer clojureListener)
           (java.util ArrayList)
           (org.antlr.v4.runtime CharStreams CommonTokenStream ParserRuleContext Token)))

;; antlr automatically prints errors to std out
;; line 1:14 token recognition error at: '\"hello @michael pink/this will work)'
;; line 1:50 extraneous input '<EOF>' expecting {'(', ')', '[', '{', ':', '::', '~'

(def default-options {:hide {:tags     #{:form :collection :literal :keyword :reader_macro :dispatch}
                             :literals #{"(" ")" "[" "]" "{" "}" "#{" "#" "^"
                                         "`" "'" "~@" "@" "#(" "#'" "#_" "#?" "#?@" "##"}}})


;; todo: mute antlr default error listener
;; todo: identify parsing errors in the tree
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
  100 times slower for this use case compared to `cons`"
  [ast rule-names]
  (if (and (instance? ParserRuleContext ast)
           ;; mainly for consistency with Js implementation
           (not-empty (.-children ast)))
    (let [head       (keyword (aget rule-names (.getRuleIndex ast)))
          wrap-child (fn [child] (hiccup child rule-names))]
      ;; attach meta data ... ala instaparse
      (with-meta (if (contains? (:tags (:hide default-options)) head)
                   (mapcat wrap-child (.-children ast))
                   (cons head (remove nil? (map wrap-child (.-children ast)))))
                 (info ast)))
    (let [text (. ast (toString))]
      (when (not (contains? (:literals (:hide default-options)) text))
        text))))


(defn parse
  [input & options]
  (let [chars      (CharStreams/fromString input)
        lexer      (new clojureLexer chars)
        tokens     (new CommonTokenStream lexer)
        parser     (new clojureParser tokens)
        rule-names (. parser (getRuleNames))
        _          (. parser (setBuildParseTree true))
        tree       (. parser (code))]
    (hiccup tree rule-names)))


;(time (parse (slurp "test/parcera/test/core.cljc")))
