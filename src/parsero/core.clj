(ns parsero.core
  (:require [instaparse.core :as instaparse]
            [clojure.string :as str]))

(def grammar
    "code: form*;

    <form>: whitespace ( literal
                        | symbol
                        | list
                        | vector
                        | map
                        | set
                        | reader-macro
                        )
            whitespace;

    whitespace = #'[,\\s]*'

    list: <'('> form* <')'> ;

    vector: <'['> form* <']'> ;

    map: map-namespace? <'{'> map-content <'}'> ;

    map-namespace: <'#'> (keyword | auto-resolve);

    map-content: (form form)*

    set: <'#{'> form* <'}'> ;

    <literal>:
          number
        | string
        | character
        | keyword
        | comment
        | symbolic
        ;

    symbolic: #'##(Inf|-Inf|NaN)'

    number: ( DOUBLE | RATIO | LONG ) !symbol (* remove ambiguity with symbols 1/5
                                                 1 -> number, / -> symbol, 5 -> number *);

    character: <'\\\\'> ( SIMPLE-CHAR | UNICODE-CHAR ) !symbol (* remove ambiguity with symbols \backspace
                                                                  \b -> character, ackspace -> symbol *);

    <reader-macro>:
          dispatch
        | metadata
        | deref
        | quote
        | backtick
        | unquote
        | unquote-splicing
        ;

    <dispatch>: <'#'> ( function | regex | var-quote | discard | tag)

    function: list;

    metadata: <'^'> ( map-metadata | shorthand-metadata );

    <map-metadata>: map form

    <shorthand-metadata>: ( symbol | string | keyword ) form;

    regex: string;

    var-quote: <'\\''> symbol;

    quote: <'\\''> form;

    backtick: <'`'> form;

    unquote: <'~'> form;

    unquote-splicing: <'~@'> form;

    deref: <'@'> form;

    discard: <'_'> form;

    tag: !'_' symbol form;

    string : <'\"'> #'[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*' <'\"'>;

    symbol: !SYMBOL-HEAD name;

    <keyword>: simple-keyword | macro-keyword ;

    auto-resolve: '::' ;

    simple-keyword: <':'> !':' name;

    macro-keyword: <auto-resolve> !':' name;

    comment: <';'> #'.*';

    (*
    ;; symbols cannot start with number, :, #
    ;; / is a valid symbol as long as it is not part of the name
    ;; note: added ' as invalid first character due to ambiguity in #'hello
    ;; -> [:tag [:symbol 'hello]]
    ;; -> [:var-quote [:symbol hello]]
    *)
    SYMBOL-HEAD: number | ':' | '#' | '\\''

    (*
    ;; NOTE: several characters are not allowed according to clojure reference.
    ;; https://clojure.org/reference/reader#_symbols
    ;; EDN reader says otherwise https://github.com/edn-format/edn#symbols
    ;; nil, true, false are actually symbols with special meaning ... not grammar rules
    ;; on their own
    VALID-CHARACTERS>: #'[\\w.*+\\-!?$%&=<>\\':#]+'
    *)
    <name>: #'([\\w.*+\\-!?$%&=<>\\':#]+\\/)?(\\/|([\\w.*+\\-!?$%&=<>\\':#]+))(?!\\/)'

    (* HIDDEN PARSERS ------------------------------------------------------ *)

    <DOUBLE>: #'[-+]?(\\d+(\\.\\d*)?([eE][-+]?\\d+)?)(M)?'

    <RATIO>: #'[-+]?(\\d+)/(\\d+)';

    <LONG>: #'[-+]?(?:(0)|([1-9]\\d*)|0[xX]([\\dA-Fa-f]+)|0([0-7]+)|([1-9]\\d?)[rR]([\\d\\w]+)|0\\d+)(N)?';

    <UNICODE-CHAR>: #'u[\\dD-Fd-f]{4}';

    <SIMPLE-CHAR>:
          'newline'
        | 'return'
        | 'space'
        | 'tab'
        | 'formfeed'
        | 'backspace'
        | #'\\P{M}\\p{M}*+'; (* https://www.regular-expressions.info/unicode.html *)")

(def clojure (instaparse/parser grammar))

#_(data/diff (first (instaparse/parses clojure (slurp "./src/parsero/core.clj")))
             (second (instaparse/parses clojure (slurp "./src/parsero/core.clj"))))

;(count (instaparse/parses clojure (slurp "./src/parsero/core.clj")))

;(time (clojure (slurp "./src/parsero/core.clj")))

;(time (clojure (slurp "./src/parsero/clojure.clj")))

;(dotimes [n 100])
;(time (clojure (slurp "./src/parsero/core.clj")))

;(time (instaparse.core/parses clojure (slurp "./resources/test_cases.clj")))

(defn- code*
  [ast ^StringBuilder string-builder]
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

    :map
    (doseq [child (rest ast)] (code* child string-builder))

    :map-namespace
    (do (. string-builder (append "#"))
        (code* (second ast) string-builder))

    :map-content
    (do (. string-builder (append "{"))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append "}")))

    :set
    (do (. string-builder (append "#{"))
        (doseq [child (rest ast)] (code* child string-builder))
        (. string-builder (append "}")))

    (:number :whitespace :symbolic :auto-resolve :symbol)
    (. string-builder (append (second ast)))

    :string
    (do (. string-builder (append "\""))
        (. string-builder (append (second ast)))
        (. string-builder (append "\"")))

    :character
    (do (. string-builder (append "\\"))
        (. string-builder (append (second ast))))

    :simple-keyword
    (do (. string-builder (append ":"))
        (. string-builder (append (second ast))))

    :macro-keyword
    (do (. string-builder (append "::"))
        (. string-builder (append (second ast))))

    :comment
    (do (. string-builder (append ";"))
        (. string-builder (append (second ast))))

    :metadata
    (do (. string-builder (append "^"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :quote
    (do (. string-builder (append "'"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :regex
    (do (. string-builder (append "#"))
        (code* (second ast) string-builder))

    :var-quote
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

    :unquote-splicing
    (do (. string-builder (append "~@"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :deref
    (do (. string-builder (append "@"))
        (doseq [child (rest ast)] (code* child string-builder)))

    :function
    (do (. string-builder (append "#"))
        (code* (second ast) string-builder))))

(defn code
  [ast]
  (let [string-builder (new StringBuilder)]
    (code* ast string-builder)
    (. string-builder (toString))))

; Successful parse.
; Profile:  {:create-node 1651, :push-full-listener 2, :push-stack 1651, :push-listener 1689, :push-result 273, :push-message 275}
; "Elapsed time: 141.452323 msecs"
#_(time (clojure (str '(ns parsero.core
                         (:require [instaparse.core :as instaparse]
                                   [clojure.data :as data]
                                   [clojure.string :as str])))
                 :trace true))
