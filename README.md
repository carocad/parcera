# parcera

[![Build Status](https://travis-ci.com/carocad/parcero.svg?branch=master)](https://travis-ci.com/carocad/parcero)

Grammar-based Clojure(script) parser.

Parcera can safely read any Clojure file without any code evaluation.

Parcera uses the wonderful [Instaparse](https://github.com/Engelberg/instaparse) as its
parsing engine and focuses entirely on the grammar definition instead. For a
full explanation of the options available for a parser please visit Instaparse website.

## usage

```clojure
(ns example.core
  (:require [parcera.core :as parcera]
            [instaparse.core :as instaparse]))

;;parse clojure code from a string
(parcera/clojure (str '(ns parsero.core
                         (:require [instaparse.core :as instaparse]
                                   [clojure.data :as data]
                                   [clojure.string :as str]))))

;; => returns a data structure with the result from the parser
[:code
 [:list
  [:symbol "ns"]
  [:whitespace " "]
  [:symbol "parsero.core"]
  [:whitespace " "]
  [:list
   [:simple-keyword "require"]
   [:whitespace " "]
   [:vector
    [:symbol "instaparse.core"]
    [:whitespace " "]
    [:simple-keyword "as"]
    [:whitespace " "]
    [:symbol "instaparse"]]
   [:whitespace " "]
   [:vector [:symbol "clojure.data"] [:whitespace " "] [:simple-keyword "as"] [:whitespace " "] [:symbol "data"]]
   [:whitespace " "]
   [:vector [:symbol "clojure.string"] [:whitespace " "] [:simple-keyword "as"] [:whitespace " "] [:symbol "str"]]]]]

;; convert an AST back into a string
(parcera/code [:symbol "ns"])
;; "ns"
```
