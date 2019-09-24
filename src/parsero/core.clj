(ns parsero.core
  (:require [instaparse.core :as instaparse]))

(def grammar
  "file: form*;

  PLACEHOLDER: #'.+';\n

  form: PLACEHOLDER
        (* literal *)
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

  ")

(def parser (instaparse/parser grammar :auto-whitespace :comma))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
