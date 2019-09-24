(ns parsero.core
  (:require [instaparse.core :as instaparse]))

(def grammar
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

  BOOLEAN : 'true' | 'false' ;")

(def parser (instaparse/parser grammar :auto-whitespace :comma))

(parser "(defn foo
          \"I don't do a whole lot.\"
          [x]
          (println x \"Hello, World!\"))")
