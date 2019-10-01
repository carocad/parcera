(ns parsero.core
  (:require [instaparse.core :as instaparse]
            [clojure.data :as data]
            [clojure.string :as str]))

(def grammar
    "code: form*;

    <form>: whitespace ( literal
                        | symbol
                        | list
                        | vector
                        | map
                        | set
                        | reader_macro
                        )
            whitespace;

    whitespace = #'[,\\s]*'

    list: <'('> form* <')'> ;

    vector: <'['> form* <']'> ;

    map: <'{'> (form form)* <'}'> ;

    set: <'#{'> form* <'}'> ;

    <literal>:
          number
        | string
        | character
        | keyword
        | comment
        ;

    number: DOUBLE | RATIO | LONG;

    character: <'\\\\'> ( SIMPLE_CHAR | UNICODE_CHAR );

    <reader_macro>:
          dispatch
        | metadata
        | deref
        | quote
        | backtick
        | unquote
        | unquote_splicing
        ;

    <dispatch>: <'#'> ( function | regex | var_quote | discard | tag)

    function: list;

    metadata: <'^'> ( map_metadata | shorthand_metadata );

    <map_metadata>: map form

    <shorthand_metadata>: ( symbol | string | keyword ) form;

    regex: string;

    var_quote: <'\\''> symbol;

    quote: <'\\''> form;

    backtick: <'`'> form;

    unquote: <'~'> form;

    unquote_splicing: <'~@'> form;

    deref: <'@'> form;

    discard: <'_'> form;

    tag: !'_' symbol form;

    string : <'\"'> #'[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*' <'\"'>;

    symbol: !SYMBOL_HEAD (VALID_CHARACTERS <'/'>)? (VALID_CHARACTERS | '/') !'/';

    <keyword>: simple_keyword | macro_keyword

    simple_keyword: <':'> !':' (VALID_CHARACTERS <'/'>)? (VALID_CHARACTERS | '/') !'/';

    macro_keyword: <'::'> !':' VALID_CHARACTERS;

    comment: <';'> #'.*';

    (* Lexers -------------------------------------------------------------- *)

    <DOUBLE>: #'[-+]?(\\d+(\\.\\d*)?([eE][-+]?\\d+)?)(M)?'

    <RATIO>: #'[-+]?(\\d+)/(\\d+)'

    <LONG>: #'[-+]?(?:(0)|([1-9]\\d*)|0[xX]([\\dA-Fa-f]+)|0([0-7]+)|([1-9]\\d?)[rR]([\\d\\w]+)|0\\d+)(N)?'
            !'.';

    <UNICODE_CHAR>: #'u[\\dD-Fd-f]{4}';

    <SIMPLE_CHAR>:
          'newline'
        | 'return'
        | 'space'
        | 'tab'
        | 'formfeed'
        | 'backspace'
        | #'.';

    (* fragments *)
    (*
    ;; symbols cannot start with number, :, #
    ;; / is a valid symbol as long as it is not part of the name
    ;; note: added ' as invalid first character due to ambiguity in #'hello
    ;; -> [:tag [:symbol hello]]
    ;; -> [:var_quote [:symbol hello]]
    *)
    SYMBOL_HEAD: number | ':' | '#' | '\\''

    (*
    ;; NOTE: several characters are not allowed according to clojure reference.
    ;; https://clojure.org/reference/reader#_symbols
    ;; EDN reader says otherwise https://github.com/edn-format/edn#symbols
    ;; nil, true, false are actually symbols with special meaning ... not grammar rules
    ;; on their own
    *)
    <VALID_CHARACTERS>: #'[\\w.*+\\-!?$%&=<>\\':#]+'")

(def clojure (instaparse/parser grammar))

#_(data/diff (first (instaparse/parses clojure (slurp "./src/parsero/core.clj")))
             (second (instaparse/parses clojure (slurp "./src/parsero/core.clj"))))

;(count (instaparse/parses clojure (slurp "./src/parsero/core.clj")))

;(time (clojure (slurp "./src/parsero/core.clj")))

;(time (clojure (slurp "./src/parsero/clojure.clj")))

;(dotimes [n 100])
;(time (clojure (slurp "./src/parsero/core.clj")))

;(time (instaparse.core/parses clojure (slurp "./resources/test_cases.clj")))

(defn code
  [ast]
  (case (first ast)
    (:code)
    (str/join "" (map code (rest ast)))

    :list
    (str "(" (str/join (map code (rest ast))) ")")

    :vector
    (str "[" (str/join (map code (rest ast))) "]")

    :map
    (str "{" (str/join (map code (rest ast))) "}")

    :set
    (str "#{" (str/join (map code (rest ast))) "}")

    (:number :whitespace)
    (second ast)

    :string
    (str "\"" (second ast) "\"")

    :symbol
    (str/join "/" (rest ast))

    :character
    (str "\\" (second ast))

    :simple_keyword
    (str ":" (str/join "/" (rest ast)))

    :macro_keyword
    (str "::" (second ast))

    :comment
    (str ";" (second ast))

    :metadata
    (str "^" (str/join (map code (rest ast))))

    :quote
    (str "'" (str/join (map code (rest ast))))

    :regex
    (str "#" (code (second ast)))

    :var_quote
    (str "#'" (code (second ast)))

    :discard
    (str "#_" (str/join (map code (rest ast))))

    :tag
    (str "#" (str/join (map code (rest ast))))
    ast))

;(code (clojure (slurp "./src/parsero/core.clj")))
;(code (clojure (slurp "./resources/test_cases.clj")))

#_(spit "resources/output.clj"
        (code (clojure (slurp "./resources/test_cases.clj"))))

;(clojure (slurp "./resources/test_cases.clj"))
;(clojure (slurp "./resources/test_cases.clj"))
