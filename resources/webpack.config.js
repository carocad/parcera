const path = require('path')

module.exports = {
  mode: "production",
  entry: path.resolve(__dirname, '../src/javascript/clojureReader.js'),
  output: {
    filename: 'clojure.reader.bundle.js',
    path: path.resolve(__dirname, '../src/javascript'),
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
