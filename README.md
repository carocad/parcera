# parcera

[![Build Status](https://travis-ci.com/carocad/parcera.svg?branch=master)](https://travis-ci.com/carocad/parcera)
[![Clojars Project](https://img.shields.io/clojars/v/carocad/parcera.svg)](https://clojars.org/carocad/parcera)

Grammar-based Clojure parser.

Parcera can safely read any Clojure file without any code evaluation.

Parcera uses the wonderful [Antlr4](https://github.com/antlr/antlr4/) as its
parsing engine and focuses entirely on the grammar definition instead.

## usage

Add `[org.antlr/antlr4-runtime "4.7.1"]` to your dependencies in addition to parcera. Parcera assumes that
this dependency will be in the classpath to avoid collisions.

```clojure
(ns example.core
  (:require [parcera.core :as parcera]))

;;parse clojure code from a string
(parcera/ast (str '(ns parcera.core
                     (:require [foo.bar :as bar]
                               [clojure.data :as data]
                               [clojure.string :as str]))))

;; => returns a data structure with the result from the parser
(:code
 (:list
  (:symbol "ns")
  (:whitespace " ")
  (:symbol "parcera.core")
  (:whitespace " ")
  (:list
   (:simple_keyword "require")
   (:whitespace " ")
   (:vector
    (:symbol "foo.bar")
    (:whitespace " ")
    (:simple_keyword "as")
    (:whitespace " ")
    (:symbol "bar"))
   (:whitespace " ")
   (:vector (:symbol "clojure.data") (:whitespace " ") (:simple_keyword "as") (:whitespace " ") (:symbol "data"))
   (:whitespace " ")
   (:vector (:symbol "clojure.string") (:whitespace " ") (:simple_keyword "as") (:whitespace " ") (:symbol "str")))))

;; convert an AST back into a string
(parcera/code [:symbol "ns"])
;; "ns"
```
