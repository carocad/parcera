
// NOTE: Antlr solves ambiguity based on the order of the rules

grammar clojure;

code: form*;

form: whitespace | literal | collection;

collection: list | vector | map;

list: '(' form* ')';

vector: '[' form* ']';

map: '{' form* '}';

literal: number | symbol | keyword | string ;// | character;

number: NUMBER;

symbol: NAME;

keyword: simple_keyword | macro_keyword;

simple_keyword: ':' NAME;

macro_keyword: '::' NAME;

string: '"' ( ~'"' | '\\' '"' )* '"';

// whitespace or comment
whitespace: SPACE+ | (SPACE* COMMENT SPACE*);

NUMBER: [+-]? DIGIT+ (DOUBLE_SUFFIX | LONG_SUFFIX | RATIO_SUFFIX);

NAME: (SIMPLE_NAME '/')? ('/' | SIMPLE_NAME );

SIMPLE_NAME: NAME_HEAD NAME_BODY*;

COMMENT: ';' ~[\r\n]*;

SPACE: [\r\n\t\f ];

// re-allow :#' as valid characters inside the name itself
fragment NAME_BODY: NAME_HEAD | [:#'];

// these is the set of characters that are allowed by all symbols and keywords
// however, this is more strict that necessary so that we can re-use it for both
fragment NAME_HEAD: ~[\r\n\t\f()[\]{}"@~^;`\\/, :#'];

fragment DOUBLE_SUFFIX: ((('.' DIGIT*)? ([eE][-+]?DIGIT+)?) 'M'?);

fragment LONG_SUFFIX: ('0'[xX]((DIGIT|[A-Fa-f])+) |
                       '0'([0-7]+) |
                       ([1-9]DIGIT?)[rR](DIGIT[a-zA-Z]+) |
                       '0'DIGIT+
                      )?'N'?;

fragment RATIO_SUFFIX: '/' DIGIT+;

fragment DIGIT: [0-9];
