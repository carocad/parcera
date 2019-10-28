
grammar clojure;

/*
 * NOTES to myself and to other developers:
 *
 * - You have to remember that the parser cannot check for semantics
 * - You have to find the right balance of dividing enforcement between the
 *   grammar and your own code.
 *
 * The parser should only check the syntax. So the rule of thumb is that when
 *  in doubt you let the parser pass the content up to your program. Then, in
 * your program, you check the semantics and make sure that the rule actually
 * have a proper meaning
 *
 * https://tomassetti.me/antlr-mega-tutorial/#lexers-and-parser
*/

code: form*;

form: whitespace | literal | collection | reader_macro;

// sets and namespaced map are not considerd collection from grammar perspective
// since they start with # -> dispatch macro
collection: list | vector | map;

list: '(' form* ')';

vector: '[' form* ']';

map: '{' form* '}';

literal: keyword | string | number | character | symbol;

keyword: simple_keyword | macro_keyword;

simple_keyword: ':' NAME;

macro_keyword: '::' NAME;

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

unquote: '~' form;

metadata: (metadata_entry whitespace?)+ ( symbol
                                        | collection
                                        | tag
                                        | unquote
                                        | unquote_splicing
                                        );

metadata_entry: '^' ( map | symbol | string | keyword );

backtick: '`' form;

quote: '\'' form;

unquote_splicing: '~@' form;

deref: '@' form;

dispatch: function
          | regex
          | set
          | conditional
          | conditional_splicing
          | namespaced_map
          | var_quote
          | discard
          | tag
          | symbolic;

function: '#(' form* ')';

regex: '#' STRING;

set: '#{' form* '}';

namespaced_map: '#' ( keyword |  '::') map;

var_quote: '#\'' symbol;

discard: '#_' form;

tag: '#' symbol whitespace? (literal | collection);

conditional: '#?(' form* ')';

conditional_splicing: '#?@(' form* ')';

symbolic: '##' ('Inf' | '-Inf' | 'NaN');

// whitespace or comment
whitespace: WHITESPACE;

NUMBER: [+-]? DIGIT+ (DOUBLE_SUFFIX | LONG_SUFFIX | RATIO_SUFFIX);

STRING: '"' ~["\\]* ('\\' . ~["\\]*)* '"';

WHITESPACE: (SPACE | COMMENT)+;

COMMENT: ';' ~[\r\n]*;

SPACE: [\r\n\t\f, ]+;

CHARACTER: '\\' (UNICODE_CHAR | NAMED_CHAR | UNICODE);

NAME: (SIMPLE_NAME '/')? ('/' | SIMPLE_NAME );

fragment UNICODE_CHAR: ~[\u0300-\u036F\u1DC0-\u1DFF\u20D0-\u20FF];

fragment NAMED_CHAR: 'newline' | 'return' | 'space' | 'tab' | 'formfeed' | 'backspace';

fragment UNICODE: 'u' [0-9d-fD-F] [0-9d-fD-F] [0-9d-fD-F] [0-9d-fD-F];

fragment SIMPLE_NAME: NAME_HEAD NAME_BODY*;

// re-allow :#' as valid characters inside the name itself
fragment NAME_BODY: NAME_HEAD | [:#'0-9];

// these is the set of characters that are allowed by all symbols and keywords
// however, this is more strict that necessary so that we can re-use it for both
fragment NAME_HEAD: ~[\r\n\t\f ()[\]{}"@~^;`\\/,:#'0-9];

fragment DOUBLE_SUFFIX: ((('.' DIGIT*)? ([eE][-+]?DIGIT+)?) 'M'?);

fragment LONG_SUFFIX: ('0'[xX]((DIGIT|[A-Fa-f])+) |
                       '0'([0-7]+) |
                       ([1-9]DIGIT?)[rR](DIGIT[a-zA-Z]+) |
                       '0'DIGIT+
                      )?'N'?;

fragment RATIO_SUFFIX: '/' DIGIT+;

fragment DIGIT: [0-9];
