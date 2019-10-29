(ns parcera.experimental
  (:import (parcera.antlr clojureParser clojureLexer clojureListener)
           (java.util ArrayList)
           (org.antlr.v4.runtime CharStreams CommonTokenStream ParserRuleContext)))

; const input = `(john :SHOUTS "hello" @michael pink/this will work)`
;const chars = new antlr4.InputStream(input)
;const lexer = new clojureLexer(chars)
;const tokens = new antlr4.CommonTokenStream(lexer)
;const parser = new clojureParser(tokens)
;const ruleNames = parser.ruleNames
;parser.buildParseTrees = true
;
;const tree = parser.code()
;console.log(JSON.stringify(treeSeq(tree, ruleNames), null, 2))
;//antlr4.tree.ParseTreeWalker.DEFAULT.walk(new listener(), tree)
;
;console.log(`DONE 💫`)

; /**
; * Takes an AST tree; the result of a parser walk and returns
; * an array with the same style as Instaparse
; *
; * @param {Object} ast
; * @param {Array<String>} ruleNames
; * @return {Array} a hiccup-like array
; */
;function treeSeq(ast, ruleNames) {
;    const result = []
;    // parser rules always have childrens
;    if (ast.children !== undefined) {
;        // we are inside a parser rule; therefore we add the rule name to the result
;        result.push(ruleNames[ast.ruleIndex])
;        result.push.apply(result, ast.children.map((child) => treeSeq(child, ruleNames)))
;        return result
;
;        // lexer rules dont have childrens, so we just take the matched text
;    } else {
;        return ast.getText()
;    }
;}

(defn- hiccup
  [ast rule-names]
  (if (and (instance? ParserRuleContext ast) (not-empty (.-children ast)))
    (cons (keyword (aget rule-names (.getRuleIndex ast)))
          (for [child (.-children ast)]
            (hiccup child rule-names)))
    (.toString ast)))


(let [input      "(john :SHOUTS \"hello\" @michael pink/this will work)"
      chars      (CharStreams/fromString input)
      lexer      (new clojureLexer chars)
      tokens     (new CommonTokenStream lexer)
      parser     (new clojureParser tokens)
      rule-names (. parser (getRuleNames))
      _          (. parser (setBuildParseTree true))
      tree       (. parser (code))]
  (hiccup tree rule-names))

