
grammar clojure;

code: form*;

form: list | symbol | string | whitespace;

list: '(' form*  ')';

symbol: (SYMBOL_NAME '/')? SYMBOL_NAME;

string: STRING;

whitespace: WHITESPACE;

STRING : '"' ( ~'"' | '\\' '"' )* '"' ;

// re-allow :#' as valid characters in a name
SYMBOL_NAME: NAME (NAME | [:#'])+;

// whitespace or comment
WHITESPACE: SPACE+ | (SPACE* ';' ~[\r\n]* SPACE*);

fragment SPACE: [\r\n\t\f ];

// these is the set of characters that are allowed by all symbols and keywords
// however, this is more strict that necessary so that we can re-use it for both
fragment NAME: ~[\r\n\t\f()[\]{}@~^;`\\/, :#'];
