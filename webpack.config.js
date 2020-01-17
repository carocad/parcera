module.exports = {
  mode: "production",
  entry: './clojureReader.js',
  output: {
    filename: 'index.bundle.js',
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
