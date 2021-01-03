const path = require('path')

module.exports = {
  mode: "production",
  entry: path.resolve(__dirname, './clojureReader.js'),
  output: {
    filename: 'clojure.reader.bundle.js',
    path: path.resolve(__dirname, '../src/clojure/parcera/antlr/js'),
    // the url to the output directory resolved relative to the HTML page
    library: "ClojureReader",
    // the name of the exported library
    libraryTarget: "commonjs2",
  },
  target: 'web',
  resolve: {
    fallback: {
        module: false,
        net: false,
        fs: false
    }
  }
}
