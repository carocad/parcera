(ns parcera.antlr.javascript
  ;; It seems that by passing antlr4 through webpack we lost the type
  ;; information. So now we get things like #object[p], #object[h], etc :(
  ;(type (:tree (parse "hello/world")))
  (:require [parcera.antlr.common :as common]
            [antlr.clojure.reader :as reader]))

; todo: enable this once I know how to make it work properly
#_(set! *warn-on-infer* true)

(defn- parser-rule-meta
  [^ParserRuleContext this]
  (let [start ^Token (.-start this)
        stop  ^Token (.-stop this)]
    (cond
      ;; happens when the parser rule is a single lexer rule
      (identical? start stop)
      {:parcera.core/start {:row    (.-line start)
                            :column (.-column start)}
       :parcera.core/end   {:row    (.-line start)
                            :column (+ (.-column start)
                                       (count (.-text start)))}}

      ;; no end found - happens on errors
      (nil? stop)
      {:parcera.core/start {:row    (.-line start)
                            :column (.-column start)}}

      :else
      {:parcera.core/start {:row    (.-line start)
                            :column (.-column start)}
       :parcera.core/end   {:row    (.-line stop)
                            :column (+ (.-column stop)
                                       (count (.-text stop)))}})))


(defn datafy
  [tree]
  (cond
    (some? (.-children tree))
    (common/map->Node {:metadata (parser-rule-meta tree)
                       :type     :parcera.core/rule
                       :rule-id  (.-ruleIndex tree)
                       :content  (.-children tree)})

    (.-isErrorNode tree)
    (let [token (.-symbol tree)]
      (common/map->Node {:type     :parcera.core/failure
                         :content  (str tree)
                         :metadata {:parcera.core/start {:row    (.-line token)
                                                         :column (.-column token)}}}))

    :else
    (common/map->Node {:type    :parcera.core/terminal
                       :content (str tree)})))


(defn parse
  [input]
  (let [chars  (reader/charStreams input)
        lexer  (doto (reader/lexer chars)
                 (.removeErrorListeners)
                 #_(.addErrorListener listener))            ;todo
        tokens (reader/tokens lexer)
        parser (reader/parser tokens)]
    (.removeErrorListeners parser)
    (set! (.-buildParseTrees parser) true)
    {:rules (into [] (map keyword) (.-ruleNames parser))
     :tree  (.code parser)}))
;:reports @(:reports listener)})) todo
