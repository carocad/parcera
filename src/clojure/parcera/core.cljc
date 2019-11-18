(ns parcera.core
  (:require [parcera.antlr.protocols :as antlr]
            #?(:clj  [parcera.antlr.java :as platform]
               :cljs [parcera.antlr.javascript :as platform]))
  #?(:cljs (:import goog.string.StringBuffer)))


(def default-hidden {:tags     #{:form :collection :literal :keyword :reader_macro :dispatch}
                     :literals #{"(" ")" "[" "]" "{" "}" "#{" "#" "^" "`" "'" "~"
                                 "~@" "@" "#(" "#'" "#_" "#?(" "#?@(" "##" ":" "::"}})

;; start and end are tokens not positions.
;; So '(hello/world)' has '(' 'hello/world' and ')' as tokens
(defn- meta-data
  "extract the match meta data information from the ast node"
  [ast]
  (let [start (antlr/start ast)
        end   (antlr/end ast)]
    (cond
      ;; happens when the parser rule is a single lexer rule
      (= start end)
      {::start {:row    (antlr/row start)
                :column (antlr/column start)}
       ::end   {:row    (antlr/row start)
                :column (.getStopIndex start)}}

      ;; no end found - happens on errors
      (nil? end)
      {::start {:row    (antlr/row start)
                :column (antlr/column start)}}

      :else
      {::start {:row    (antlr/row start)
                :column (antlr/column start)}
       ::end   {:row    (antlr/row end)
                  :column (antlr/column end)}})))


;; for some reason cljs doesnt accept escaping the / characters
(def name-pattern #?(:clj  #"^([^\s\/]+\/)?(\/|[^\s\/]+)$"
                     :cljs #"^([^\s/]+/)?(/|[^\s/]+)$"))


(defn- failure
  "Checks that `rule` conforms to additional rules which are too difficult
  to represent with pure Antlr4 syntax"
  [rule children metadata]
  (case rule
    (:symbol :simple_keyword :macro_keyword)
    (when (nil? (re-find name-pattern (first children)))
      (with-meta (list ::failure (cons rule children))
                 (assoc-in metadata [::start :message]
                           (str "name cannot contain more than one /"))))

    :map
    (let [forms (remove (comp #{:whitespace :discard} first) children)]
      (when (odd? (count forms))
        (with-meta (list ::failure (cons rule children))
                   (assoc-in metadata [::start :message]
                             "Map literal must contain an even number of forms"))))

    :set
    (let [forms         (remove (comp #{:whitespace :discard} first) children)
          set-length    (count forms)
          unique-length (count (distinct forms))]
      (when (not= set-length unique-length)
        (with-meta (list ::failure (cons rule children))
                   (assoc-in metadata [::start :message]
                             "Set literal contains duplicate forms"))))

    nil))


(defn- hiccup
  "transforms the tree `hiccup-like` ast data structure.

  Yields a lazy sequence to avoid expensive computation whenever
  the user is not interested in the full content."
  [tree rule-names hide-tags hide-literals]
  (cond
    (boolean (satisfies? antlr/ParserRule tree))
    (let [rule     (get rule-names (antlr/rule-index tree))
          children (for [child (antlr/children tree)
                         :let [child (hiccup child rule-names hide-tags hide-literals)]
                         :when (not (nil? child))]
                     child)
          ;; attach meta data ... ala instaparse
          ast-meta (meta-data tree)
          ;; extra validation rules
          fail     (failure rule children ast-meta)]
      ;; parcera hidden tags are always "or" statements, so just take the single children
      (if (contains? hide-tags rule)
        (first children)
        (or fail (with-meta (cons rule children) ast-meta))))

    (boolean (satisfies? antlr/ErrorNode tree))
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


(defn ast
  "Clojure (antlr4) parser. It can be used as:
  - `(parcera/ast input-string)`
     -> returns a lazy AST representation of input-string

   The following options are accepted:
   - `:unhide` can be one of `#{:tags :content :all}`. Defaults to `nil`

   NOTE: Antlr returns a fully parsed version of the provided input string
       however this function returns a lazy sequence in order to expose
       those through Clojure's immutable data structures"
  [input & {:as options}]
  (let [hidden     (unhide options)
        {:keys [parser errors]} (platform/parser input)
        rule-names (antlr/rules parser)
        tree       (antlr/tree parser)
        result     (hiccup tree rule-names (:tags hidden) (:literals hidden))
        reports    @(:reports (:parser errors))]
    (vary-meta result assoc ::errors reports)))


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


(defn failure?
  "Checks if ast contains any `::failure` instances.

  NOTE: This function is potentially slow since it might have to check the
  complete ast to be sure that there are no failures.

  Whenever possible, prefer to handle errors directly appearing in the ast"
  [ast]
  (or
    ;; ast is root node
    (not (empty? (::errors (meta ast))))
    ;; ast is child node
    (and (seq? ast) (= ::failure (first ast)))
    ;; ast is root node but "doesnt know" about the failure -> conformed
    (some #{::failure} (filter keyword? (tree-seq seq? identity ast)))))

#_(time (ast (str '(ns parcera.core
                     (:require [instaparse.core :as instaparse]
                               [clojure.data :as data]
                               [clojure.string :as str])))))

#_(time (ast "(ns parcera.core
              (:require [instaparse.core :as #{:hello \"world\" :hello}]
                        [clojure.data :as data]
                        [clojure.string :as str])"))

#_(filter :meta (map #(hash-map :item % :meta (meta %))
                     (tree-seq seq? seq (ast "
      (ns
        parcera.core))"))))
