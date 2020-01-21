/**
 * Utility module to load all antlr related functionality; including
 * the generated one.
 *
 * This file will then be passed through Webpack to create a single
 * bundle
 */
const antlr4 = require('antlr4')
const {ClojureLexer} = require('../src/clojure/parcera/antlr/js/ClojureLexer')
const {ClojureParser} = require('../src/clojure/parcera/antlr/js/ClojureParser')

const reader = {
    charStreams: (input) => new antlr4.CharStreams.fromString(input),
    lexer: (chars) => new ClojureLexer(chars),
    tokens: (lexer) => new antlr4.CommonTokenStream(lexer),
    parser: (tokens) => new ClojureParser(tokens),
}

module.exports = reader
// global.ClojureReader = reader

// console.log(`DONE 💫`)
