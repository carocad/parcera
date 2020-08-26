
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
input: ignore | form ;

ignore: whitespace | comment | discard;

form: literal | collection | reader_macro;

// sets and namespaced map are not considerd collection from grammar perspective
// since they start with # -> dispatch macro
collection: list | vector | map;

list: '(' input* ')';

vector: '[' input* ']';

map: '{' input* '}';

literal: keyword | macro_keyword | string | number | character | symbol;

keyword: KEYWORD;

macro_keyword: MACRO_KEYWORD;

string: STRING;

number: NUMBER;

character: CHARACTER;

/*
 * rules NOT captured in this statement:
 * - a symbol cannot start with a number "9.5hello"
 * - a symbol cannot be followed by another symbol "hello/world/" -> "hello/world" "/"
 */
symbol: SYMBOL;

reader_macro: ( unquote
              | metadata
              | backtick
              | quote
              | dispatch
              | unquote_splicing
              | deref
              );

metadata: ((metadata_entry | deprecated_metadata_entry) ignore*)+
          ( symbol
          | collection
          | set
          | namespaced_map
          | tag
          | fn
          | unquote
          | unquote_splicing
          | conditional
          | conditional_splicing
          | deref
          | quote
          | backtick
          | var_quote
          );

metadata_entry: '^' ignore* ( map | symbol | string | keyword | macro_keyword | conditional);

/**
 * According to https://github.com/clojure/clojure-site/blob/7493bdb10222719923519bfd6d2699a26677ee82/content/guides/weird_characters.adoc#-and----metadata
 * the declaration `#^` is deprecated
 *
 * In order to support roundtrip of parser rules it is required to exactly identify the
 * character used which would not be possible with something like '#'? '^'
 */
deprecated_metadata_entry: '#^' ignore* ( map | symbol | string | keyword | macro_keyword | conditional);

backtick: '`' ignore* form;

quote: '\'' ignore* form;

unquote: '~' ignore* form;

unquote_splicing: '~@' ignore* form;

deref: '@' ignore* form;

dispatch: ( fn
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

fn: '#' list; // no whitespace allowed

regex: '#' STRING;

set: '#{' input* '}'; // no whitespace allowed

namespaced_map: '#' (keyword | macro_keyword | auto_resolve)
                    ignore*
                    map;

auto_resolve: '::';

var_quote: '#\'' ignore* form;

discard: '#_' ignore* form;

tag: '#' symbol ignore* (literal | collection | tag);

conditional: '#?' whitespace* list;

conditional_splicing: '#?@' whitespace* list;

/* This definition allows arbitrary symbolic values; following
 * on LispReader to just read the form and throw if the symbol
 * is not known.
 */
symbolic: '##' ignore* SYMBOL;

// I assume symbol and list from lisp reader, but tools.reader seems to
// indicate something else
eval: '#=' ignore* (symbol | list | conditional);

whitespace: WHITESPACE;

comment: COMMENT;

NUMBER: [+-]? DIGIT+ (DOUBLE_SUFFIX | LONG_SUFFIX | RATIO_SUFFIX);

STRING: '"' ~["\\]* ('\\' . ~["\\]*)* '"';

WHITESPACE: [\r\n\t\f, ]+;

COMMENT: (';' | '#!') ~[\r\n]*;

CHARACTER: '\\' (NAMED_CHAR | UNICODE | OCTAL | UNICODE_CHAR);

// note: ::/ is NOT a valid macro keyword, unlike :/
MACRO_KEYWORD: '::' KEYWORD_HEAD KEYWORD_BODY*;

/*
 * Example -> :http://www.department0.university0.edu/GraduateCourse52
 *
 * technically that is NOT a valid keyword. However in order to maintain
 * backwards compatibility the Clojure team didnt remove it from LispReader
 */
KEYWORD: ':' ((KEYWORD_HEAD KEYWORD_BODY*) | '/');

/**
 * a symbol must start with a valid character and can be followed
 * by more "relaxed" character restrictions
 *
 * This pattern matches things like: hello, hello/world, /hello/world/
 * that is by design. Parcera's grammar is more permissive than Clojure's
 * since otherwise Antlr would parse hello/world/ as
 * [:symbol "hello/world"] [:symbol "/"]
 * which is also wrong but more difficult to identify when looking at the AST
 */
SYMBOL: NAME_HEAD NAME_BODY*;

fragment UNICODE_CHAR: ~[\u0300-\u036F\u1DC0-\u1DFF\u20D0-\u20FF];

fragment NAMED_CHAR: 'newline' | 'return' | 'space' | 'tab' | 'formfeed' | 'backspace';

fragment UNICODE: 'u' [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F];

fragment OCTAL: 'o' ([0-7]) | ([0-7] [0-7]) | ([0-3] [0-7] [0-7]);

fragment KEYWORD_BODY: KEYWORD_HEAD | [:/];

fragment KEYWORD_HEAD: ALLOWED_NAME_CHARACTER | [#'];

// symbols can contain : # ' as part of their names
fragment NAME_BODY: NAME_HEAD | [:#'];

fragment NAME_HEAD: ALLOWED_NAME_CHARACTER | [/];

// these is the set of characters that are allowed by all symbols and keywords
// however, this is more strict that necessary so that we can re-use it for both
fragment ALLOWED_NAME_CHARACTER: ~[\r\n\t\f ()[\]{}"@~^;`\\,:#'/];

fragment DOUBLE_SUFFIX: ((('.' DIGIT*)? ([eE][-+]?DIGIT+)?) 'M'?);

// check LispReader for the pattern used by Clojure
fragment LONG_SUFFIX: ( [xX][0-9A-Fa-f]+
                      | [0-7]+
                      | [rR][0-9a-zA-Z]+
                      )? 'N'?;

fragment RATIO_SUFFIX: '/' DIGIT+;

fragment DIGIT: [0-9];
