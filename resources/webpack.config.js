/**
 * NOTE: I feel this more as a hack than as an actual solution.
 *
 * Ideally I should just let ClojureScript import the lexer and parser
 * from a browser perspective. However it seems that the Closure compiler
 * doesnt like conditional imports so it throws saying that 'fs' is not
 * defined which is clearly a node.js dependency.
 *
 * On the other hand, if I pass the module as node.js target then the Closure
 * compiler corrupts the source code which makes it unusable :(
 */
module.exports = {
  mode: "production",
  entry: '../src/javascript/clojureReader.js',
  output: {
    filename: 'clojure.reader.bundle.js',
    path: '../src/javascript'
    // the url to the output directory resolved relative to the HTML page
    library: "ClojureReader",
    // the name of the exported library
    libraryTarget: "commonjs2",
  },
  target: 'web',
  node: {
    module: "empty",
    net: "empty",
    fs: "empty" }
}
