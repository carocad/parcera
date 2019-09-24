(ns parsero.core
  (:require [instaparse.core :as instaparse]))

(def grammar
  "file: form* EOF;

  form: literal
      | list
      | vector
      | map
      | reader_macro
      ;

  forms: form* ;

  list: '(' forms ')' ;

  vector: '[' forms ']' ;

  map: '{' (form form)* '}' ;

  set: '#{' forms '}' ;

  reader_macro:
        lambda
      | meta_data
      | regex
      | var_quote
      | host_expr
      | set
      | tag
      | discard
      | dispatch
      | deref
      | quote
      | backtick
      | unquote
      | unquote_splicing
      | gensym
      ;

  (* TJP added '&' (gather a variable number of arguments) *)
  quote: '\\'' form;

  backtick: '`' form;

  unquote: '~' form;

  unquote_splicing: '~@' form;

  tag: '^' form form;

  deref: '@' form;

  gensym: SYMBOL '#';

  lambda: '#(' form* ')';

  meta_data: '#^' (map form | form);

  var_quote: '#\\'' symbol;

  host_expr: '#+' form form;

  discard: '#_' form;

  dispatch: '#' symbol form;

  regex: '#' STRING;

  literal:
        STRING
      | number
      | character
      | NIL
      | BOOLEAN
      | keyword
      | symbol
      | param_name
      ;

  hex: HEX;
  bin: BIN;
  bign: BIGN;
  number: #'\\d*'
        (* FLOAT
      | hex
      | bin
      | bign
      | LONG
      *);

  character:
        (* named_char
      | u_hex_quad
      | *) any_char
      ;

  named_char: CHAR_NAMED ;

  any_char: CHAR_ANY ;

  u_hex_quad: CHAR_U ;

  keyword: macro_keyword | simple_keyword;

  simple_keyword: ':' symbol;

  macro_keyword: ':' ':' symbol;

  symbol: ns_symbol | simple_sym;
  simple_sym: SYMBOL;
  ns_symbol: NS_SYMBOL;

  param_name: PARAM_NAME;

  (* Lexers *)
  (* -------------------------------------------------------------------- *)

  STRING : #'^\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"';

  (* FIXME: Doesn't deal with arbitrary read radixes, BigNums *)
  FLOAT
      : '-'? [0-9]+ FLOAT_TAIL
      | '-'? 'Infinity'
      | '-'? 'NaN'
      ;

  FLOAT_TAIL
      : FLOAT_DECIMAL FLOAT_EXP
      | FLOAT_DECIMAL
      | FLOAT_EXP
      ;

  FLOAT_DECIMAL
      : '.' [0-9]+
      ;

  FLOAT_EXP
      : [eE] '-'? [0-9]+
      ;

  HEXD: [0-9a-fA-F] ;

  HEX: '0' [xX] HEXD+ ;

  BIN: '0' [bB] [10]+ ;

  LONG: '-'? [0-9]+[lL]?;

  BIGN: '-'? [0-9]+[nN];

  CHAR_U: '\\' 'u'[0-9D-Fd-f] HEXD HEXD HEXD ;

  CHAR_NAMED:
              '\\' ( 'newline'
             | 'return'
             | 'space'
             | 'tab'
             | 'formfeed'
             | 'backspace' ) ;

  CHAR_ANY: #'\\\\.*';

  NIL : 'nil';

  BOOLEAN : 'true' | 'false' ;

  SYMBOL:
        '.'
      | '/'
      | NAME
      ;

  NS_SYMBOL: NAME '/' SYMBOL;

  PARAM_NAME: '%' ((('1'..'9')('0'..'9')*)|'&')? ;

  (* Fragments *s)
  (* -------------------------------------------------------------------- *)

  NAME: SYMBOL_HEAD SYMBOL_REST* (':' SYMBOL_REST+)* ;

  SYMBOL_HEAD
      : ~('0' .. '9'
          (* FIXME: could be one group *)
          | '^' | '`' | '\\'' | '\"' | '#' | '~' | '@' | ':' | '/' | '%' | '(' | ')' | '[' | ']' | '{' | '}'
          | [ \n\r\t,] (* FIXME: could be WS *)
          )
      ;

  SYMBOL_REST
    : SYMBOL_HEAD
    | '0'..'9'
    | '.'
    ;

  COMMENT: ';' ~[\r\n]* ;")

(def parser (instaparse/parser grammar :auto-whitespace :comma))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
