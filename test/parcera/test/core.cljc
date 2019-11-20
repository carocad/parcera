(ns parcera.test.core
  (:require [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check :as tc]
            [parcera.core :as parcera]
            #?(:cljs [parcera.slurp :refer [slurp]])))


(defn- roundtrip
  "checks parcera can parse and write back the exact same input code"
  [input]
  (= input (parcera/code (parcera/ast input))))


(defn- valid?
  [input]
  (not (parcera/failure? (parcera/ast input))))


;; todo: is this even possible with antlr ? 🤔
#_(defn- clear
    [input]
    (= 1 (count (instaparse/parses parcera/ast input :unhide :all))))


(def validity
  "The grammar definition of parcera is valid for any clojure value. Meaning
  that for any clojure value, parcera can create an AST for it"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (valid? input)))


(def symmetric
  "The read <-> write process of parcera MUST be symmetrical. Meaning
  that the AST and the text representation are equivalent"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (roundtrip input)))


#_(def unambiguous
    "The process of parsing clojure code yields consistent results. Meaning
  that any input should (but must not) only have 1 AST representation ... however
  I have found this is not always possible"
    (prop/for-all [input (gen/fmap pr-str gen/any)]
      (clear input)))


(deftest simple
  (testing "character literals"
    (as-> "\\t" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "\\n" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "\\r" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "\\a" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "\\é" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "\\ö" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "\\ï" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "\\ϕ" input (and (is (valid? input))
                           (is (roundtrip input))))))


(deftest metadata
  (testing "simple definitions"
    (let [input    ":bar"
          ast      (parcera/ast input)
          location (meta (second ast))]
      (is (= (:row (::parcera/start location)) 1))
      (is (= (:column (::parcera/start location)) 0))
      (is (= (:row (::parcera/start location)) 1))
      (is (= (:column (::parcera/end location))
             (count input)))))

  (testing "syntax error"
    (let [input    "hello/world/"
          ast      (parcera/ast input)
          location (meta (second ast))]
      (is (parcera/failure? ast))
      (is (some? (:message (::parcera/start location)))))))


(deftest data-structures
  (testing "grammar definitions"
    (let [result (tc/quick-check 200 validity)]
      (is (:pass? result)
          (str "read process failed at\n"
               (with-out-str (pprint/pprint result))))))

  (testing "clojure values"
    (let [result (tc/quick-check 200 symmetric)]
      (is (:pass? result)
          (str "read <-> write process yield different result. Failed at\n"
               (with-out-str (pprint/pprint result))))))

  #_(testing "very little ambiguity"
      (let [result (tc/quick-check 200 unambiguous)]
        (is (:pass? result)
            (str "high ambiguity case found. Please check the grammar to ensure "
                 "high accuracy\n"
                 (with-out-str (pprint/pprint result)))))))


(deftest unit-tests
  (testing "names"
    (as-> "foo" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "foo-bar" input (and (is (valid? input))
                               (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "foo->bar" input (and (is (valid? input))
                                (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "->" input (and (is (valid? input))
                          (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "->as" input (and (is (valid? input))
                            (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "föl" input (and (is (valid? input))
                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "Öl" input (and (is (valid? input))
                          (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "ϕ" input (and (is (valid? input))
                         (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "❤️" input (and (is (valid? input))
                          (is (roundtrip input))))))
;(is (clear input))))))


(deftest edge-cases
  (testing "comments"
    (as-> "{:hello ;2}
           2}" input (and (is (valid? input))
                          (is (roundtrip input)))))
  ;(is (clear input)))))
  (testing "symbols"
    (as-> "hello/world/" input (is (not (valid? input))))
    (as-> ":hello/world/" input (is (not (valid? input))))
    (as-> "::hello/world/" input (is (not (valid? input)))))

  (testing "strings"
    (as-> "hello \"world" input (is (not (valid? input))))))


(deftest macros
  (testing "metadata"
    (as-> "^String [a b 2]" input (and (is (valid? input))
                                       (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "^\"String\" [a b 2]" input (and (is (valid? input))
                                           (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "^:string [a b 2]" input (and (is (valid? input))
                                        (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "^{:a 1} [a b 2]" input (and (is (valid? input))
                                       (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "^:hello ^\"World\" ^{:a 1} [a b 2]" input (and (is (valid? input))
                                                          (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "discard"
    (as-> "#_[a b 2]" input (and (is (valid? input))
                                 (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "#_(a b 2)" input (and (is (valid? input))
                                 (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "#_{:a 1}" input (and (is (valid? input))
                                (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "#_macros" input (and (is (valid? input))
                                (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "regex"
    (as-> "#_\"[a b 2]\"" input (and (is (valid? input))
                                     (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "comments"
    (as-> ";[a b 2]" input (and (is (valid? input))
                                (is (roundtrip input))))
    ;(is (clear input))))
    (as-> ";; \"[a b 2]\"" input (and (is (valid? input))
                                      (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "2 ;[a b 2]" input (and (is (valid? input))
                                  (is (roundtrip input))))
    ;(is (clear input))))
    (as-> " :hello ;; \"[a b 2]\"" input (and (is (valid? input))
                                              (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "var quote"
    (as-> "#'hello/world" input (and (is (valid? input))
                                     (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "#'/" input (and (is (valid? input))
                           (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "tag"
    (as-> "#hello/world [1 a \"3\"]" input (and (is (valid? input))
                                                (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "#hello/world {1 \"3\"}" input (and (is (valid? input))
                                              (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "keyword"
    (as-> "::hello/world [1 a \"3\"]" input (and (is (valid? input))
                                                 (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "::hello" input (and (is (valid? input))
                               (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "quote"
    (as-> "'hello/world" input (and (is (valid? input))
                                    (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "'hello" input (and (is (valid? input))
                              (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "'/" input (and (is (valid? input))
                          (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "backtick"
    (as-> "`hello/world" input (and (is (valid? input))
                                    (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "`hello" input (and (is (valid? input))
                              (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "`/" input (and (is (valid? input))
                          (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "unquote"
    (as-> "~hello/world" input (and (is (valid? input))
                                    (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "~(hello 2 3)" input (and (is (valid? input))
                                    (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "~/" input (and (is (valid? input))
                          (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "quote splicing"
    (as-> "~@hello/world" input (and (is (valid? input))
                                     (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "~@(hello 2 b)" input (and (is (valid? input))
                                     (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "deref"
    (as-> "@hello/world" input (and (is (valid? input))
                                    (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "@hello" input (and (is (valid? input))
                              (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "@/" input (and (is (valid? input))
                          (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "anonymous function"
    (as-> "#(= (str %1 %2 %&))" input (and (is (valid? input))
                                           (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "namespaced map"
    (as-> "#::{:a 1 b 3}" input (and (is (valid? input))
                                     (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "#::hello{:a 1 b 3}" input (and (is (valid? input))
                                          (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "reader conditional"
    (as-> "#?(:clj Double/NaN :cljs js/NaN :default nil)" input
          (and (is (valid? input))
               (is (roundtrip input))))
    ;(is (clear input))))
    (as-> "[1 2 #?@(:clj [3 4] :cljs [5 6])]" input
          (and (is (valid? input))
               (is (roundtrip input))))))
;(is (clear input))))))


(deftest bootstrap

  (testing "parcera should be able to parse itself"
    (let [input (slurp "./src/clojure/parcera/core.cljc")]
      (and (is (valid? input))
           (is (roundtrip input))))
    ;(is (clear input))))
    (let [input (slurp "./src/clojure/parcera/slurp.cljc")]
      (and (is (valid? input))
           (is (roundtrip input)))))
  ;(is (clear input)))))

  (testing "parcera should be able to parse its own test suite"
    (let [input (slurp "./test/parcera/test/core.cljc")]
      (and (is (valid? input))
           (is (roundtrip input))))
    ;(is (clear input))))
    (let [input (slurp "./test/parcera/test/benchmark.clj")]
      (and (is (valid? input))
           (is (roundtrip input))))))
;(is (clear input))))))


(deftest clojure$cript

  (testing "parcera should be able to parse clojure core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojure/master/src/clj/clojure/core.clj")]
      (time (is (= core-content (parcera/code (parcera/ast core-content :optimize :memory)))))))

  (testing "parcera should be able to parse clojurescript core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojurescript/master/src/main/clojure/cljs/core.cljc")]
      (time (is (= core-content (parcera/code (parcera/ast core-content :optimize :memory))))))))
