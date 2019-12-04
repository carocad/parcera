(ns parcera.core
  (:require [clojure.core.protocols :as clojure]
            [clojure.spec.alpha :as s]
            #?(:clj [parcera.antlr.java :as platform]))
  ; todo: re-enable once we have javscript support
  ;:cljs [parcera.antlr.javascript :as platform]))
  #?(:cljs (:import goog.string.StringBuffer)))


(def default-hidden {:tags     #{:form :collection :literal :keyword :reader_macro :dispatch}
                     :literals #{"(" ")" "[" "]" "{" "}" "#{" "#" "^" "`" "'" "~"
                                 "~@" "@" "#(" "#'" "#_" "#?(" "#?@(" "##" ":" "::"}})


;; a name can contain a maximum of 1 /
(def qualified-name #?(:clj  #"^([^\s\/]+\/)?(\/|[^\s\/]+)$"
                       ;; for some reason cljs doesnt accept escaping the / characters
                       :cljs #"^([^\s/]+/)?(/|[^\s/]+)$"))


(defn- qualified-name? [text] (re-find qualified-name text))

;; a symbol cannot start with a number
(defn- symbol-number-start? [text] (re-find #"^[+-]?\d+" text))

(defn- keep-forms [coll] (remove (comp #{:whitespace :discard} first) coll))

(s/def ::symbol (s/and string?
                       qualified-name?
                       (complement symbol-number-start?)))

(s/def ::simple_keyword (s/and string? qualified-name?))

(s/def ::macro_keyword (s/and string?
                              qualified-name?
                              (complement #{"/"})))

(s/def ::map (s/and (s/conformer keep-forms)
                    #(even? (count %))))

(s/def ::set (s/and (s/conformer keep-forms)
                    #(= (count %) (count (distinct %)))))

(defn- report
  "utility to avoid repeating this code over and over again"
  [rule children metadata message]
  (with-meta (list ::failure (cons rule children))
             (assoc-in metadata [::start :message] message)))


(defn- failure
  "Checks that `rule` conforms to additional rules which are too difficult
  to represent with pure Antlr4 syntax"
  [rule children metadata]
  (case rule
    :symbol
    (when (string? (first children))
      (when (not (s/valid? ::symbol (first children)))
        (report rule children metadata
                (s/explain-str ::symbol (first children)))))

    :simple_keyword
    (when (string? (first children))
      (when (not (s/valid? ::simple_keyword (first children)))
        (report rule children metadata
                (s/explain-str ::simple_keyword (first children)))))

    :macro_keyword
    (when (string? (first children))
      (when (not (s/valid? ::macro_keyword (first children)))
        (report rule children metadata
                (s/explain-str ::macro_keyword (first children)))))

    :map
    (when (not (s/valid? ::map children))
      (report rule children metadata (s/explain-str ::map children)))

    :set
    (when (not (s/valid? ::set children))
      (report rule children metadata (s/explain-str ::set children)))

    nil))


(defn- hiccup
  "transforms the tree `hiccup-like` ast data structure.

  Yields a lazy sequence to avoid expensive computation whenever
  the user is not interested in the full content."
  [tree rule-names hide-tags hide-literals]
  (let [node (clojure/datafy tree)]
    (case (:type node)
      ::rule
      (let [rule     (get rule-names (:rule-id node))
            children (for [child (:content node)
                           :let [child (hiccup child rule-names hide-tags hide-literals)]
                           :when (not (nil? child))]
                       child)
            ;; extra validation rules
            fail     (failure rule children (:metadata node))]
        (if (contains? hide-tags rule)
          ;; parcera hidden tags are always "or" statements, so just take the single children
          (first children)
          ;; attach meta data ... ala instaparse
          (or fail (with-meta (cons rule children) (:metadata node)))))

      ::failure
      (with-meta (list ::failure (:content node))
                 (:metadata node))

      ::terminal
      (if (contains? hide-literals (:content node)) nil (:content node)))))


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
        {:keys [rules tree errors]} (platform/parse input)
        result     (hiccup tree rules (:tags hidden) (:literals hidden))
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

    :simple_keyword
    (do (. string-builder (append ":"))
        (. string-builder (append (second ast))))

    :macro_keyword
    (do (. string-builder (append "::"))
        (. string-builder (append (second ast))))

    :symbolic
    (do (. string-builder (append "##"))
        (. string-builder (append (second ast))))

    :regex
    (do (. string-builder (append "#"))
        (. string-builder (append (second ast))))

    :auto_resolve
    (. string-builder (append "::"))

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
