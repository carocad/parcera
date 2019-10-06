(ns parcera.test.core
  (:require [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check :as tc]
            [parcera.core :as parcera]
            [instaparse.core :as instaparse]))

(def validity
  "The grammar definition of parcera is valid for any clojure value. Meaning
  that for any clojure value, parcera can create an AST for it"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= false (instaparse/failure? (parcera/clojure input)))))


(def symmetric
  "The read <-> write process of parcera MUST be symmetrical. Meaning
  that the AST and the text representation are equivalent"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= input (parcera/code (parcera/clojure input)))))


(def unambiguous
  "The process of parsing clojure code yields consistent results. Meaning
  that any input should (but must not) only have 1 AST representation ... however
  I have found this is not always possible"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (= 1 (count (instaparse/parses parcera/clojure
                                   input
                                   :unhide :all)))))


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

  (testing "very little ambiguity"
    (let [result (tc/quick-check 200 unambiguous)]
      (is (:pass? result)
          (str "high ambiguity case found. Please check the grammar to ensure "
               "high accuracy\n"
               (with-out-str (pprint/pprint result)))))))


(deftest macros
  (testing "metadata"
    (as-> "^String [a b 2]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "^\"String\" [a b 2]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "^:string [a b 2]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "^{:a 1} [a b 2]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "^:hello ^\"World\" ^{:a 1} [a b 2]" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "discard"
    (as-> "#_[a b 2]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "#_(a b 2)" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "#_{:a 1}" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "#_macros" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "regex"
    (as-> "#_\"[a b 2]\"" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "comments"
    (as-> ";[a b 2]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> ";; \"[a b 2]\"" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "var quote"
    (as-> "#'hello/world" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "#'/" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "tag"
    (as-> "#hello/world [1 a \"3\"]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "#hello/world {1 \"3\"}" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "keyword"
    (as-> "::hello/world [1 a \"3\"]" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "::hello" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "quote"
    (as-> "'hello/world" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "'hello" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "'/" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "backtick"
    (as-> "`hello/world" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "`hello" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "`/" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "unquote"
    (as-> "~hello/world" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "~(hello 2 3)" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "~/" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "quote splicing"
    (as-> "~@hello/world" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "~@(hello 2 b)" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "deref"
    (as-> "@hello/world" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "@hello" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "@/" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "anonymous function"
    (as-> "#(= (str %1 %2 %&))" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "namespaced map"
    (as-> "#::{:a 1 b 3}" input (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "#::hello{:a 1 b 3}" input (is (= input (parcera/code (parcera/clojure input))))))

  (testing "reader conditional"
    (as-> "#?(:clj Double/NaN :cljs js/NaN :default nil)" input
          (is (= input (parcera/code (parcera/clojure input)))))
    (as-> "[1 2 #?@(:clj [3 4] :cljs [5 6])]" input
          (is (= input (parcera/code (parcera/clojure input)))))))


(deftest bootstrap

  (testing "parcera should be able to parse itself"
    (let [core-content (slurp "./src/parcera/core.clj")]
      (is (= core-content (parcera/code (parcera/clojure core-content))))))

  (testing "parcera should be able to parse its own test suite"
    (let [test-content (slurp "./test/parcera/test/core.clj")]
      (is (= test-content (parcera/code (parcera/clojure test-content)))))))


(deftest clojure$cript

  (testing "parcera should be able to parse clojure core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojure/master/src/clj/clojure/core.clj")]
      (time (is (= core-content (parcera/code (parcera/clojure core-content :optimize :memory)))))))

  (testing "parcera should be able to parse clojurescript core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojurescript/master/src/main/clojure/cljs/core.cljc")]
      (time (is (= core-content (parcera/code (parcera/clojure core-content :optimize :memory))))))))
