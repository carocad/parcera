(ns parcera.experimental
  (:import (parcera.antlr clojureParser clojureLexer clojureListener)
           (java.util ArrayList)
           (org.antlr.v4.runtime CharStreams CommonTokenStream ParserRuleContext Token)))

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
  [ast rule-names]
  (if (and (instance? ParserRuleContext ast)
           ;; mainly for consistency with Js implementation
           (not-empty (.-children ast)))
    (let [head [(keyword (aget rule-names (.getRuleIndex ast)))]
          body (for [child (.-children ast)]
                 (hiccup child rule-names))]
      ;; attach meta data ... ala instaparse
      (with-meta (into head body) (info ast)))
    (. ast (toString))))


(let [input      "(john :SHOUTS \"hello\" @michael pink/this will work)"
      chars      (CharStreams/fromString input)
      lexer      (new clojureLexer chars)
      tokens     (new CommonTokenStream lexer)
      parser     (new clojureParser tokens)
      rule-names (. parser (getRuleNames))
      _          (. parser (setBuildParseTree true))
      tree       (. parser (code))]
  (hiccup tree rule-names))

