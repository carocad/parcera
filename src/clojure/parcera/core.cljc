(ns parcera.core
  (:require [parcera.antlr.protocols :as antlr]
            [parcera.antlr.java :as platform]
            [clojure.zip :as zip])
  #?(:cljs (:import goog.string.StringBuffer)))


(defn- branches
  "given a zipper loc returns all reachable branch nodes"
  [loc]
  (filter zip/branch?
          (take-while (complement zip/end?)
                      (iterate zip/next loc))))


(defn- lookahead
  "given an AST yields a sequence of branches which match rule and are
  followed by the ahead rules"
  [ast rule ahead]                                          ;; ahead -> #{:rule-names}
  (let [zipper (zip/seq-zip ast)]
    (for [branch (branches zipper)
          :when (= rule (first (zip/node branch)))
          :let [neighbour (zip/right branch)]
          :when (some? neighbour)
          :when (ahead (first (zip/node neighbour)))]
      branch)))


(defn- negative-lookahead
  "given an AST yields a sequence of branches which match rule and are
  followed by the forbidden rules"
  [ast rule forbidden]                                      ;; ahead -> #{:rule-names}
  (lookahead ast rule (complement forbidden)))


(def default-hidden {:tags     #{:form :collection :literal :keyword :reader_macro :dispatch}
                     :literals #{"(" ")" "[" "]" "{" "}" "#{" "#" "^" "`" "'" "~"
                                 "~@" "@" "#(" "#'" "#_" "#?(" "#?@(" "##" ":" "::"}})


(defn- info
  "extract the match meta data information from the ast node"
  [ast]
  (let [start (antlr/start ast)
        end   (antlr/end ast)]
    {::start {:row    (antlr/row start)
              :column (antlr/column start)}
     ::end   {:row    (antlr/row end)
              :column (antlr/column end)}}))


(defn- hiccup
  "transform the AST into a `hiccup-like` data structure.

  This function doesnt return a vectors because they are
  100 times slower for this use case compared to `cons` cells"
  [tree rule-names hide-tags hide-literals]
  (cond
    (satisfies? antlr/ParserRule tree)
    (let [rule         (keyword (get rule-names (antlr/rule-index tree)))
          children-ast (for [child (antlr/children tree)
                             :let [child-ast (hiccup child rule-names hide-tags hide-literals)]
                             :when (not (nil? child-ast))]
                         child-ast)
          ast          (if (contains? hide-tags rule)
                         (apply concat children-ast)
                         (cons rule children-ast))]
      ;; attach meta data ... ala instaparse
      (with-meta ast (info tree)))

    (satisfies? antlr/ErrorNode tree)
    (let [token (antlr/token tree)
          ;; error metadata
          info  {::start {:row    (antlr/row token)
                          :column (antlr/column token)}}]
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


(defn clojure
  "Clojure (antlr4) parser. It can be used as:
  - `(parcera/clojure input-string)`
     -> returns an AST representation of input-string

   The following options are accepted:
   - `:unhide` can be one of `#{:tags :content :all}`. Defaults to `nil`
   - `:total`  thruthy value to get a parse tree even on failures"
  [input & {:as options}]
  (let [hidden     (unhide options)
        {:keys [parser listener]} (platform/parser input)
        rule-names (antlr/rules parser)
        tree       (antlr/tree parser)]
    (if (or (empty? @(:reports listener)) (:total options))
      (hiccup tree rule-names (:tags hidden) (:literals hidden))
      @(:reports listener))))
;; todo: expose a proper error record ?


(defn- code*
  "internal function used to imperatively build up the code from the provided
   AST as Clojure's str would be too slow"
  [ast #?(:clj  ^StringBuilder string-builder
          :cljs ^StringBuffer string-builder)]
  (case (first ast)
    :code
    (doseq [child (rest ast)]
      (code* child string-builder))

    :list
    (do (. string-builder (append "("))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append ")")))

    :vector
    (do (. string-builder (append "["))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append "]")))

    :namespaced_map
    (do (. string-builder (append "#"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :map
    (do (. string-builder (append "{"))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append "}")))

    :set
    (do (. string-builder (append "#{"))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append "}")))

    (:number :whitespace :symbol :character :string)
    (. string-builder (append (second ast)))

    :symbolic
    (do (. string-builder (append "##"))
        (. string-builder (append (second ast))))

    :regex
    (do (. string-builder (append "#"))
        (. string-builder (append (second ast))))

    :auto_resolve
    (. string-builder (append "::"))

    :simple_keyword
    (do (. string-builder (append ":"))
        (. string-builder (append (second ast))))

    :macro_keyword
    (do (. string-builder (append "::"))
        (. string-builder (append (second ast))))

    :metadata
    (do (doseq [child (rest (butlast ast))] (code* child string-builder))
        (code* (last ast) string-builder))

    :metadata_entry
    (doseq [child (rest ast)]
      (. string-builder (append "^"))
      (code* child string-builder))

    :quote
    (do (. string-builder (append "'"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :var_quote
    (do (. string-builder (append "#'"))
        (code* (second ast) string-builder))

    :discard
    (do (. string-builder (append "#_"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :tag
    (do (. string-builder (append "#"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :backtick
    (do (. string-builder (append "`"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :unquote
    (do (. string-builder (append "~"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :unquote_splicing
    (do (. string-builder (append "~@"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :conditional
    (do (. string-builder (append "#?("))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append ")")))

    :conditional_splicing
    (do (. string-builder (append "#?@("))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append ")")))

    :deref
    (do (. string-builder (append "@"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :function
    (do (. string-builder (append "#("))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append ")")))))


(defn code
  "Transforms your AST back into code

   ast: The nested sequence of [:keyword & content] which MUST follow the
        same structure as the result of `(parcera/clojure input-string)`

   Returns a string representation of the provided AST

   In general (= input (parcera/code (parcera/clojure input)))"
  [ast]
  (let [string-builder #?(:clj (new StringBuilder)
                          :cljs (new StringBuffer))]
    (code* ast string-builder)
    (. string-builder (toString))))

;; this is just forwarding for the time
;; ideally we shouldnt need to do it but directly define it here
(defn failure? [obj] (platform/failure? obj))

; Successful parse.
; Profile:  {:create-node 384, :push-full-listener 2, :push-stack 384,
;            :push-listener 382, :push-result 227, :push-message 227 }
; "Elapsed time: 47.25084 msecs"
#_(time (clojure (str '(ns parcera.core
                         (:require [instaparse.core :as instaparse]
                                   [clojure.data :as data]
                                   [clojure.string :as str])))))

#_(let [input    "hello/world/"
        ast      (time (clojure input))
        failures (negative-lookahead ast :symbol :symbol)]
    (for [branch failures]
      (let [neighbour (zip/right branch)
            failure   (zip/replace branch (list ::failure
                                                (zip/node branch)
                                                (zip/node neighbour)))
            removal   (zip/remove (zip/right failure))]
        (zip/root removal))))
