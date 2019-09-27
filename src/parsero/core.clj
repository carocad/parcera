(ns parsero.core
  (:require [instaparse.core :as instaparse]
            [clojure.core.strint :as strint]))

(def name-regex "(?![0-9])[\\w\\*\\+\\!\\-\\'\\?\\>\\<\\=]+")
(def namespace-regex (strint/<< "~{name-regex}(\\.~{name-regex})*"))
(def namespaced-symbol-regex (strint/<< "(~{namespace-regex}\\/)?~{name-regex}"))

parsero.core/namespace-regex
(def grammar
  (strint/<<
    "file: form*;

  form:
        literal
      | list
      | vector
      | map
      (* | reader_macro *)
      ;

  forms: form* ;

  list: '(' forms ')' ;

  vector: '[' forms ']' ;

  map: '{' (form form)* '}' ;

  set: '#{' forms '}' ;

  literal:
        STRING
      (* | number *)
      (* | character *)
      | NIL
      | BOOLEAN
      (* | keyword *)
      (* | symbol *)
      (* | param_name *)
      ;

  (* Lexers *)
  (* -------------------------------------------------------------------- *)

  STRING : #'^\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"';

  NIL : 'nil';

    BOOLEAN : 'true' | 'false' ;

    SIMPLE_SYMBOL: #'${name-regex}';

    NAMESPACED_SYMBOL: #'~{namespaced-symbol-regex}';

    PARAM_NAME: '%' ((('1'..'9')('0'..'9')*)|'&')? ;

    COMMENT: ';' ~[\r\n]* ;"))

(def parser (instaparse/parser grammar :auto-whitespace :comma))

(parser "(defn foo
          \"I don't do a whole lot.\"
          [x]
          (println x \"Hello, World!\"))")
