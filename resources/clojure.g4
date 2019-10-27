
grammar clojure;

code: form*;

form: list | symbol | string | whitespace;

list: '(' form*  ')';

symbol: (NAME '/')? NAME;

string: STRING;

whitespace: WHITESPACE;

STRING : '"' ( ~'"' | '\\' '"' )* '"' ;

NAME: ~[\r\n\t\f()[\]{}@~^;`\\/, ];

// whitespace or comment
WHITESPACE: SPACE+ | (SPACE* ';' SPACE);

fragment SPACE: [\r\n\t\f ];
