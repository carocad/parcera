(ns parcera.antlr.javascript
  ;; It seems that by passing antlr4 through webpack we lost the type
  ;; information. So now we get things like #object[p], #object[h], etc :(
  ;(type (:tree (parse "hello/world")))
  (:require [parcera.antlr.common :as common]
            [antlr.clojure.reader :as reader]))

; todo: enable this once I know how to make it work properly
#_(set! *warn-on-infer* true)

;; See parcera.antlr.java for a proper explanation of this
;; HACK: I dont know of an elegant way to get the same result as I have a pass an object
;; to antlr with the methods below
(defn- AntlrFailure
  [reports]
  #js {:reportAmbiguity
                (fn [recognizer dfa start-index stop-index exact ambig-alts configs]
                  (println "report ambiguity: " recognizer dfa start-index stop-index exact ambig-alts configs))

       :reportAttemptingFullContext
                (fn [recognizer dfa start-index stop-index conflicting-alts configs]
                  (println "report attempting full context: " recognizer dfa start-index stop-index conflicting-alts configs))

       :reportContextSensitivity
                (fn [recognizer dfa start-index stop-index prediction configs]
                  (println "report context sensitivity: " recognizer dfa start-index stop-index prediction configs))

       :syntaxError
                (fn [recognizer offending-symbol line column message error]
                  ;; recognizer is either clojureParser or clojureLexer
                  (let [report (merge {:row     line
                                       :column  column
                                       :message message
                                       ;; buildParseTrees is true/false; regardless if it is there recognizer is a parser
                                       :type    (if (some? (.-buildParseTrees recognizer)) :parser :lexer)}
                                      (when (some? (.-buildParseTrees recognizer))
                                        {:symbol (str offending-symbol)
                                         :stack  (->> (.getRuleInvocationStack recognizer)
                                                      (reverse)
                                                      (map keyword))})
                                      (when (some? error)
                                        {:error error}))]
                    (vswap! reports conj report)))

       :reports reports})


(defn- parser-rule-meta
  [^ParserRuleContext this]
  (let [start ^Token (.-start this)
        stop  ^Token (.-stop this)]
    (if (nil? stop)                                         ;; no end found - happens on errors
      {:parcera.core/start {:row    (.-line start)
                            :column (.-column start)}}
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
  (let [listener (AntlrFailure (volatile! ()))
        chars    (reader/charStreams input)
        lexer    (doto (reader/lexer chars)
                   (.removeErrorListeners)
                   (.addErrorListener listener))
        tokens   (reader/tokens lexer)
        parser   (doto (reader/parser tokens)
                   (.removeErrorListeners)
                   (.addErrorListener listener))]
    (set! (.-buildParseTrees parser) true)
    {:rules   (into [] (map keyword) (.-ruleNames parser))
     :tree    (.code parser)
     :reports @(.-reports listener)}))
