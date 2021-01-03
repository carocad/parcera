(ns parcera.antlr.javascript
  ;; It seems that by passing antlr4 through webpack we lost the type
  ;; information. So now we get things like #object[p], #object[h], etc :(
  ;(type (:tree (parse "hello/world")))
  (:require [antlr.clojure.reader :refer [ClojureReader]]))

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
    (if (nil? stop)
      ;; no end found - happens on errors
      {:parcera.core/start {:row    (.-line start)
                            :column (.-column start)}}
      {:parcera.core/start {:row    (.-line start)
                            :column (.-column start)}
       :parcera.core/end   {:row    (.-line stop)
                            :column (+ (.-column stop)
                                       (count (.-text stop)))}})))


(defn ast
  [tree rule-names hide-tags hide-literals]
  (cond
    (some? (.-children tree))
    (let [meta     (parser-rule-meta tree)
          rule     (get rule-names (.-ruleIndex tree))
          children (sequence (comp (map #(ast % rule-names hide-tags hide-literals))
                                   (remove nil?))
                             (.-children tree))]
      (if (contains? hide-tags rule)
        ;; parcera hidden tags are always "or" statements, so just take the single children
        (first children)
        ;; attach meta data ... ala instaparse
        (with-meta (cons rule children) meta)))

    (.-isErrorNode tree)
    (let [token (.-symbol tree)]
      (with-meta (list :parcera.core/failure (:content (str tree)))
                 {:parcera.core/start {:row    (.getLine token)
                                       :column (.getCharPositionInLine token)}}))

    :else
    (let [content (str tree)]
      (when-not (contains? hide-literals content)
        content))))


(defn parse
  [input]
  (let [listener (AntlrFailure (volatile! ()))
        chars    (.charStreams ClojureReader input)
        lexer    (doto (.lexer ClojureReader chars)
                   (.removeErrorListeners)
                   (.addErrorListener listener))
        tokens   (.tokens ClojureReader lexer)
        parser   (doto (.parser ClojureReader tokens)
                   (.removeErrorListeners)
                   (.addErrorListener listener))]
    (set! (.-buildParseTrees parser) true)
    {:rules   (into [] (map keyword) (.-ruleNames parser))
     :tree    (.code parser)
     :reports @(.-reports listener)}))
