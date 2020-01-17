const antlr4 = require('antlr4')
const {ClojureLexer} = require('./src/javascript/parcera/antlr/ClojureLexer')
const {ClojureParser} = require('./src/javascript/parcera/antlr/ClojureParser')

const reader = {
    charStreams: (input) => new antlr4.CharStreams.fromString(input),
    lexer: (chars) => new ClojureLexer(chars),
    tokens: (lexer) => new antlr4.CommonTokenStream(lexer),
    parser: (tokens) => new ClojureParser(tokens),
}

module.exports = reader
// global.ClojureReader = reader

console.log(`DONE 💫`)
