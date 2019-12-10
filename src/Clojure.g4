
grammar Clojure;

/*
 * NOTES to myself and to other developers:
 *
 * - You have to remember that the parser cannot check for semantics
 * - You have to find the right balance of dividing enforcement between the
 *   grammar and your own code.
 *
 * The parser should only check the syntax. So the rule of thumb is that when
 * in doubt you let the parser pass the content up to your program. Then, in
 * your program, you check the semantics and make sure that the rule actually
 * have a proper meaning
 *
 * https://tomassetti.me/antlr-mega-tutorial/#lexers-and-parser
*/

code: input* EOF;

// useful rule to differentiate actual clojure content from anything else
input: whitespace | comment | discard | form ;

form: literal | collection | reader_macro;

// sets and namespaced map are not considerd collection from grammar perspective
// since they start with # -> dispatch macro
collection: list | vector | map;

list: '(' input* ')';

vector: '[' input* ']';

map: '{' input* '}';

literal: keyword | string | number | character | symbol;

keyword: simple_keyword | macro_keyword;
/**
 * keywords are treated like symbols prepended by : this was 'borrowed' from
 * Clojure's Lisp Reader which uses a single regex to match both and then
 * checks if it starts with :
 *
 * I am not fully sure if it would be better to make keywords Lexer rules but
 * at least for the time being this approach seems to work quite well
 */
simple_keyword: ':' (NAME | NUMBER);

macro_keyword: '::' (NAME | NUMBER);

string: STRING;

number: NUMBER;

character: CHARACTER;

symbol: NAME;

reader_macro: ( unquote
              | metadata
              | backtick
              | quote
              | dispatch
              | unquote_splicing
              | deref
              );

metadata: ((metadata_entry | deprecated_metadata_entry) whitespace?)+ ( symbol
                                                                      | collection
                                                                      | tag
                                                                      | unquote
                                                                      | unquote_splicing
                                                                      );

metadata_entry: '^' ( map | symbol | string | keyword );

/**
 * According to https://github.com/clojure/clojure-site/blob/7493bdb10222719923519bfd6d2699a26677ee82/content/guides/weird_characters.adoc#-and----metadata
 * the declaration `#^` is deprecated
 *
 * In order to support roundtrip of parser rules it is required to exactly identify the
 * character used which would not be possible with something like `'#'? '^'`
 */
deprecated_metadata_entry: '#^' ( map | symbol | string | keyword );

backtick: '`' whitespace? form;

quote: '\'' whitespace? form;

unquote: '~' whitespace? form;

unquote_splicing: '~@' whitespace? form;

deref: '@' whitespace? form;

dispatch: ( function
          | regex
          | set
          | conditional
          | conditional_splicing
          | namespaced_map
          | var_quote
          | tag
          | symbolic
          | eval
          );

function: '#(' input* ')';

regex: '#' STRING;

set: '#{' input* '}';

namespaced_map: '#' ( keyword |  auto_resolve) whitespace? map;

auto_resolve: '::';

var_quote: '#\'' whitespace? form;

discard: '#_' (whitespace? discard)? whitespace? form;

tag: '#' symbol whitespace? (literal | collection | tag);

conditional: '#?(' input* ')';

conditional_splicing: '#?@(' input* ')';

symbolic: '##' ('Inf' | '-Inf' | 'NaN');

// I assume symbol and list from lisp reader, but tools.reader seems to indicate something else
eval: '#=' whitespace? (symbol | list);

whitespace: WHITESPACE;

comment: COMMENT;

NUMBER: [+-]? DIGIT+ (DOUBLE_SUFFIX | LONG_SUFFIX | RATIO_SUFFIX);

STRING: '"' ~["\\]* ('\\' . ~["\\]*)* '"';

WHITESPACE: [\r\n\t\f, ]+;

COMMENT: (';' | '#!') ~[\r\n]*;

CHARACTER: '\\' (UNICODE_CHAR | NAMED_CHAR | UNICODE);

/**
 * note: certain patterns are allowed on purpose because it would be too difficult
 * to validate those with antlr; parcera takes care of those special cases
 */
NAME: NAME_HEAD NAME_BODY*;

fragment UNICODE_CHAR: ~[\u0300-\u036F\u1DC0-\u1DFF\u20D0-\u20FF];

fragment NAMED_CHAR: 'newline' | 'return' | 'space' | 'tab' | 'formfeed' | 'backspace';

fragment UNICODE: 'u' [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F];

// symbols can contain : # ' as part of their names
fragment NAME_BODY: NAME_HEAD | [#':0-9];

// these is the set of characters that are allowed by all symbols and keywords
// however, this is more strict that necessary so that we can re-use it for both
fragment NAME_HEAD: ~[\r\n\t\f ()[\]{}"@~^;`\\,:#'];

fragment DOUBLE_SUFFIX: ((('.' DIGIT*)? ([eE][-+]?DIGIT+)?) 'M'?);

// check LispReader for the pattern used by Clojure
fragment LONG_SUFFIX: ( [xX][0-9A-Fa-f]+
                      | [0-7]+
                      | [rR][0-9a-zA-Z]+
                      )? 'N'?;

fragment RATIO_SUFFIX: '/' DIGIT+;

fragment DIGIT: [0-9];
