
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

number: (OCTAL | HEXADECIMAL | RADIX | RATIO | LONG | DOUBLE);

character: (NAMED_CHAR | OCTAL_CHAR | UNICODE_CHAR | UNICODE);

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

// check LispReader for the patterns used to match numbers
OCTAL: SIGN? ZERO [0-7]+ BIG_INT?;

HEXADECIMAL: SIGN? ZERO [xX][0-9A-Fa-f]+ BIG_INT?;

// radix cannot be read as a big int? 🤔 is this a bug in LispReader?
RADIX: SIGN? ([2-9] | ([1-2][0-9]) | ('3'[0-6]))
             [rR]
             [0-9a-zA-Z]+;

RATIO: SIGN? DIGIT+ '/' DIGIT+;

LONG: SIGN? DECIMAL BIG_INT?;

fragment BIG_INT: 'N';

DOUBLE: SIGN? DECIMAL ('.' DIGIT*)? ([eE]SIGN?DIGIT+)? 'M'?;

fragment DECIMAL: (ZERO | ([1-9] DIGIT*));

fragment ZERO: '0';

STRING: '"' ~["\\]* ('\\' . ~["\\]*)* '"';

// any unicode whitespace "character"
WHITESPACE: [\p{White_Space},]+;

COMMENT: (';' | '#!') ~[\r\n]*;

NAMED_CHAR: ESCAPE ('newline' | 'return' | 'space' | 'tab' | 'formfeed' | 'backspace');

// This is supposed to be the JavaScript friendly version of #'\P{M}\p{M}*+'
// mentioned here: https://www.regular-expressions.info/unicode.html
// It's cooked by this generator: http://kourge.net/projects/regexp-unicode-block
// ticking all 'Combining Diacritical Marks' boxes *))
UNICODE_CHAR: ESCAPE ~[\u0300-\u036F\u1DC0-\u1DFF\u20D0-\u20FF];

UNICODE: ESCAPE 'u' [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F];

// octal character must be between 0 and 377
// https://github.com/clojure/clojure/blob/06097b1369c502090be6489be27cc280633cb1bd/src/jvm/clojure/lang/LispReader.java#L604
// https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
OCTAL_CHAR: ESCAPE 'o' ([0-7] | ([0-7] [0-7]) | ([0-3] [0-7] [0-7]));

fragment ESCAPE: '\\';


// ::/ is NOT a valid macro keyword, unlike :/
MACRO_KEYWORD: '::' KEYWORD_WILDCARD;

KEYWORD: ':' ('/' | KEYWORD_WILDCARD);

fragment KEYWORD_WILDCARD: KEYWORD_HEAD // a single character like :+ :>
                           // a keyword cannot end in : nor /
                           | (KEYWORD_HEAD KEYWORD_HEAD)
                           // multiple : and / are allowed inside keywords for backward compatibility
                           // Example -> :http://www.department0.university0.edu/GraduateCourse52
                           | (KEYWORD_HEAD KEYWORD_BODY+ KEYWORD_HEAD);

fragment KEYWORD_BODY: KEYWORD_HEAD | ':' | '/';

fragment KEYWORD_HEAD: ALLOWED_NAME_CHARACTER | DIGIT | [#'] | SIGN;


SYMBOL: SIMPLE_SYMBOL ('/' SIMPLE_SYMBOL)?;

fragment SIMPLE_SYMBOL: '/' // edge case; / is also the namespace separator
                        // a single character like + - / etc
                        | (ALLOWED_NAME_CHARACTER | SIGN)
                        // a symbol that starts with +- cannot be followed by a number
                        | (SIGN SYMBOL_HEAD SYMBOL_BODY*)
                        // a symbol that doesnt start with +- can be followed by a number like 't2#'
                        | (ALLOWED_NAME_CHARACTER (SYMBOL_HEAD | DIGIT | ':') SYMBOL_BODY*);

// symbols can contain : # ' as part of their names
fragment SYMBOL_BODY: SYMBOL_HEAD | DIGIT | ':';

fragment SYMBOL_HEAD: ALLOWED_NAME_CHARACTER | [#'] | SIGN;


// https://stackoverflow.com/a/15503680
// used to avoid the parser matching a single invalid token as the composition
// of two valid tokens. Examples:
// +9hello -> [:number +9] [:symbol hello]
// \o423 -> [:character \o43] [:number 2]
SENTINEL: ESCAPE? (ALLOWED_NAME_CHARACTER | DIGIT | SIGN | [/])+;

fragment KEYWORD_BODY: KEYWORD_HEAD | [:/];

fragment KEYWORD_HEAD: ALLOWED_NAME_CHARACTER | DIGIT | [#'] | SIGN;

// symbols can contain : # ' as part of their names
fragment SYMBOL_BODY: SYMBOL_HEAD | DIGIT | ':';

fragment SYMBOL_HEAD: ALLOWED_NAME_CHARACTER | [#'/] | SIGN;

// these is the set of characters that are allowed by all symbols and keywords
// however, this is more strict that necessary so that we can re-use it for both
fragment ALLOWED_NAME_CHARACTER: ~[\p{White_Space},()[\]{}"@~^;`\\:#'/0-9+-];

fragment SIGN: [+-];

fragment DIGIT: [0-9];
