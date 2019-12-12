(ns parcera.test.core
  (:require [clojure.pprint :as pprint]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check :as tc]
            [parcera.core :as parcera]
            #?(:cljs [parcera.slurp :refer [slurp]])))


(defn- nested?
  [coll]
  (not= 1 (count (tree-seq coll? (fn [entry] (filter coll? entry)) coll))))


(defn- monotonic*
  "given a nested ast check that the parent node contains the inner nodes
  that is, for every child its row/column information must be inside the
  parent row/column range"
  [nested-ast]
  (let [start  (::parcera/start (meta nested-ast))
        end    (::parcera/end (meta nested-ast))
        starts (map ::parcera/start (map meta (rest nested-ast)))
        ends   (map ::parcera/end (map meta (rest nested-ast)))]
    (and (apply <= (map :row starts))
         (apply <= (map :column starts))
         (apply <= (map :row ends))
         (apply <= (map :column ends))
         (<= (:row start) (:row (first starts)))
         (<= (:column start) (:column (first starts)))
         (<= (:row end) (:row (last ends)))
         (<= (:column end) (:column (last ends))))))


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


(def monotonic
  "for any given AST the children of a parser rule are always
  inside its row/column range"
  (prop/for-all [input (gen/fmap pr-str gen/any)]
    (let [ast (parcera/ast input)]
      (map monotonic* (filter nested? (tree-seq seq? seq ast))))))



#_(def unambiguous
    "The process of parsing clojure code yields consistent results. Meaning
  that any input should (but must not) only have 1 AST representation ... however
  I have found this is not always possible"
    (prop/for-all [input (gen/fmap pr-str gen/any)]
      (clear input)))

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

  (testing "parsed metadata"
    (let [result (tc/quick-check 200 monotonic)]
      (is (:pass? result)
          (str "inconsistent meta data found. Failed at\n"
               (with-out-str (pprint/pprint result))))))
  #_(testing "very little ambiguity"
      (let [result (tc/quick-check 200 unambiguous)]
        (is (:pass? result)
            (str "high ambiguity case found. Please check the grammar to ensure "
                 "high accuracy\n"
                 (with-out-str (pprint/pprint result)))))))


(deftest unit-tests
  (testing "character literals"
    (let [input "\\t"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "\\n"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "\\r"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "\\a"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "\\é"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "\\ö"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "\\ï"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "\\ϕ"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "\ua000"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "\u000a"]
      (is (valid? input))
      (is (roundtrip input))))

  (testing "AST metadata"
    (let [input    ":bar"
          ast      (parcera/ast input)
          location (meta (second ast))]
      (is (= (:row (::parcera/start location)) 1))
      (is (= (:column (::parcera/start location)) 0))
      (is (= (:row (::parcera/start location)) 1))
      (is (= (:column (::parcera/end location))
             (count input)))))

  (testing "AST metadata failure message"
    (let [input    "hello/world/"
          ast      (parcera/ast input)
          location (meta (second ast))]
      (is (parcera/failure? ast))
      (is (some? (:message (::parcera/start location))))))

  (testing "symbols"
    (let [input "foo"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "foo-bar"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "foo->bar"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "->"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "->as"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "föl"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "Öl"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "ϕ"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "❤️"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "hello/world/"]
      (is (not (valid? input))))
    (let [input ":hello/world/"]
      (is (not (valid? input))))
    (let [input "::hello/world/"]
      (is (not (valid? input)))))
  ;(is (clear input))))))

  (testing "strings"
    (let [input "hello \"world"]
      (is (not (valid? input)))))

  (testing "tag literals"
    ;; nested tag literals
    (let [input "#a #b 1"]
      (is (valid? input))))

  (testing "keywords"
    ;; a keyword can be a simple number because its first character is : which is
    ;; NOT a number ;)
    (let [input ":1"]
      (is (valid? input))
      (is (roundtrip input)))
    ;; ::/ is valid according to parcera's lexer but not for Clojure
    (let [input "::/"]
      (is (not (valid? input))))
    (let [input "::hello/world [1 a \"3\"]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "::hello"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input ":http://www.department0.university0.edu/GraduateCourse52"]
      (is (valid? input))
      (is (roundtrip input))))

  (testing "numbers"
    (let [input "0x1f"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "2r101010"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "8r52"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "36r16"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "22/7"]
      (is (valid? input))
      (is (roundtrip input))))

  (testing "metadata"
    (let [input "^String [a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "^\"String\" [a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "^:string [a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "^{:a 1} [a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "^:hello ^\"World\" ^{:a 1} [a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;; DEPRECATED meta data macro style
    (let [input "(meta #^{:a 10} #^String {})"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "discard"
    (let [input "#_[a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "#_(a b 2)"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "#_{:a 1}"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "#_macros"]
      (is (valid? input))
      (is (roundtrip input)))
    ;; discard statements can be "nested"
    (let [input "#_#_:a :b"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "#_#_ :a"]
      (is (not (valid? input)))))

  (testing "regex"
    (let [input "#_\"[a b 2]\""]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "comments"
    (let [input "{:hello ;2}
                   2}"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input ";[a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input ";; \"[a b 2]\""]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "2 ;[a b 2]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input " :hello ;; \"[a b 2]\""]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input " :hello #! \"[a b 2]\""]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "#! invalid { input '"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "var quote"
    (let [input "#'hello/world"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "#'/"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "tag"
    (let [input "#hello/world [1 a \"3\"]"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "#hello/world {1 \"3\"}"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "quote"
    (let [input "'hello/world"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "'hello"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "'/"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "backtick"
    (let [input "`hello/world"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "`hello"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "`/"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "unquote"
    (let [input "~hello/world"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "~(hello 2 3)"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "~/"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "quote splicing"
    (let [input "~@hello/world"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "~@(hello 2 b)"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "deref"
    (let [input "@hello/world"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "@hello"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "@/"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "anonymous function"
    (let [input "#(= (str %1 %2 %&))"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "namespaced map"
    (let [input "#::{:a 1 b 3}"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "#::hello{:a 1 b 3}"]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "reader conditional"
    (let [input "#?(:clj Double/NaN :cljs js/NaN :default nil)"]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input "[1 2 #?@(:clj [3 4] :cljs [5 6])]"]
      (is (valid? input))
      (is (roundtrip input))))

  (testing "whitespace"
    (let [input "(defmacro x [a] `   #'  ~  '  a)"]
      (is (valid? input))
      (is (roundtrip input))))

  (testing "eval"
    (let [input "#=  (inc 1)"]
      (is (valid? input))
      (is (roundtrip input)))
    (let [input "#=inc"]
      (is (valid? input))
      (is (roundtrip input))))

  (testing "EOF"
    (let [input ":hello \"  "]
      (is (not (valid? input))))))
;(is (clear input))))))


(deftest bootstrap

  (testing "parcera should be able to parse itself"
    (let [input (slurp "./src/clojure/parcera/core.cljc")]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input (slurp "./src/clojure/parcera/slurp.cljc")]
      (is (valid? input))
      (is (roundtrip input))))
  ;(is (clear input)))))

  (testing "parcera should be able to parse its own test suite"
    (let [input (slurp "./test/parcera/test/core.cljc")]
      (is (valid? input))
      (is (roundtrip input)))
    ;(is (clear input))))
    (let [input (slurp "./test/parcera/test/benchmark.clj")]
      (is (valid? input))
      (is (roundtrip input)))))
;(is (clear input))))))


(deftest clojure$cript

  (testing "parcera should be able to parse clojure core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojure/master/src/clj/clojure/core.clj")]
      (time (is (= core-content (parcera/code (parcera/ast core-content :optimize :memory)))))))

  (testing "parcera should be able to parse clojurescript core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojurescript/master/src/main/clojure/cljs/core.cljc")]
      (time (is (= core-content (parcera/code (parcera/ast core-content :optimize :memory))))))))


;; when in doubt enable the test below. I parses clojure reader test suite so, if we
;; expect something to work it probably will be tested there.
#_(testing "parcera should be able to parse clojurescript core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojure/master/test/clojure/test_clojure/reader.cljc")]
      (for [form (tree-seq seq? seq (parcera/ast core-content :optimize :memory))
            :when (coll? form)
            :when (contains? #{::parcera/failure} (first form))]
        [form (meta form)])))
