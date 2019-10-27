
grammar clojure;

code: form*;

form: list | symbol | keyword | string | whitespace;

list: '(' form*  ')';

symbol: NAME;

keyword: simple_keyword | macro_keyword;

simple_keyword: ':' NAME;

macro_keyword: '::' NAME;

string: '"' ( ~'"' | '\\' '"' )* '"';

whitespace: WHITESPACE;

NAME: (SIMPLE_NAME '/')? SIMPLE_NAME;

SIMPLE_NAME: NAME_HEAD NAME_BODY+;

// whitespace or comment
WHITESPACE: SPACE+ | (SPACE* ';' ~[\r\n]* SPACE*);

fragment SPACE: [\r\n\t\f ];

// re-allow :#' as valid characters inside the name itself
fragment NAME_BODY: NAME_HEAD | [:#'];

// these is the set of characters that are allowed by all symbols and keywords
// however, this is more strict that necessary so that we can re-use it for both
fragment NAME_HEAD: ~[\r\n\t\f()[\]{}@~^;`\\/, :#'];
