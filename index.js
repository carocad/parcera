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
    // a parser rule has childrens if it is a repetition (* or +)
    if (ast.children !== undefined) {
        // we are inside a parser rule; therefore we add the rule name to the result
        result.push(ruleNames[ast.ruleIndex])
        for (const child of ast.children) {
            const childResult = treeSeq(child, ruleNames)
            // we are on a lexer match so we just add the value and move on
            if (child.getPayload().tokenIndex !== undefined) {
                result.push(childResult)
            } else if (child.getPayload().ruleIndex !== undefined) {
                result.push.apply(result, childResult)
            } else {
                throw new Error(`Unexpected ast node: ${child.toString()}`)
            }
        }
        return result

        // the parser rule its not a repetition -> it matches directly
        // therefore we just take the match
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
