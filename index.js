const antlr4 = require('antlr4/index')
const {clojureLexer} = require('./build/js/clojureLexer')
const {clojureParser} = require('./build/js/clojureParser')
const {clojureListener} = require('./build/js/clojureListener')

/**
 * Takes an AST tree; the result of a parser walk and returns
 * an array with the same style as Instaparse
 *
 * @param {Object} ast
 * @param {Array<String>} ruleNames
 * @return {Array} a hiccup-like array
 */
function treeSeq(ast, ruleNames) {
    const result = []
    // parser rules always have childrens
    if (ast.children !== undefined) {
        // we are inside a parser rule; therefore we add the rule name to the result
        result.push(ruleNames[ast.ruleIndex])
        result.push.apply(result, ast.children.map((child) => treeSeq(child, ruleNames)))
        return result

        // lexer rules dont have childrens, so we just take the matched text
    } else {
        return ast.getText()
    }
}

const input = `(john :SHOUTS "hello" @michael pink/this will work)`
const chars = new antlr4.InputStream(input)
const lexer = new clojureLexer(chars)
const tokens = new antlr4.CommonTokenStream(lexer)
const parser = new clojureParser(tokens)
const ruleNames = parser.ruleNames
parser.buildParseTrees = true

const tree = parser.code()
console.log(JSON.stringify(treeSeq(tree, ruleNames), null, 2))
//antlr4.tree.ParseTreeWalker.DEFAULT.walk(new listener(), tree)

console.log(`DONE 💫`)
