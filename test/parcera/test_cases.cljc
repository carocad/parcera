(ns parcera.test-cases
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


(defmacro roundtrip
  "checks parcera can parse and write back the exact same input code"
  [input]
  `(is (= ~input (parcera/code (parcera/ast ~input)))))


(defmacro valid?
  [input]
  `(is (not (parcera/failure? (parcera/ast ~input)))))


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
               (with-out-str (pprint/pprint result)))))))


(deftest character-literals
  (let [input "\\t"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "\\n"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "\\r"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "\\a"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "\\é"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "\\ö"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "\\ï"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "\\ϕ"]
    (valid? input)
    (roundtrip input))
  (let [input "\ua000"]
    (valid? input)
    (roundtrip input))
  (let [input "\u000a"]
    (valid? input)
    (roundtrip input)))


(deftest AST-metadata
  (let [input    ":bar"
        ast      (parcera/ast input)
        location (meta (second ast))]
    (is (= (:row (::parcera/start location)) 1))
    (is (= (:column (::parcera/start location)) 0))
    (is (= (:row (::parcera/start location)) 1))
    (is (= (:column (::parcera/end location))
           (count input)))))


(deftest symbols
  (let [input "foo"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "foo-bar"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "foo->bar"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "->"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "->as"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "föl"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "Öl"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "ϕ"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "❤️"]
    (valid? input)
    (roundtrip input))
  #_(let [input "hello/world/"]
      (is (not (valid? input))))
  #_(let [input ":hello/world/"]
      (is (not (valid? input))))
  #_(let [input "::hello/world/"]
      (is (not (valid? input)))))
;(is (clear input))))))

(deftest tag-literals
  ;; nested tag literals
  (let [input "#a #b 1"]
    (valid? input)))


(deftest keywords
  ;; a keyword can be a simple number because its first character is : which is
  ;; NOT a number ;)
  (let [input ":1"]
    (valid? input)
    (roundtrip input))
  (let [input ":/"]
    (valid? input)
    (roundtrip input))
  (let [input "::/"]
    (is (parcera/failure? (parcera/ast input))))
  (let [input "::hello/world [1 a \"3\"]"]
    (valid? input)
    (roundtrip input))
  (let [input "::hello"]
    (valid? input)
    (roundtrip input))
  (let [input ":#hello"]
    (valid? input)
    (roundtrip input))
  ;; this is NOT a valid literal keyword but it is "supported" by the current
  ;; reader
  (let [input ":http://www.department0.university0.edu/GraduateCourse52"]
    (valid? input)
    (roundtrip input)))


(deftest numbers
  (let [input "0x1f"]
    (valid? input)
    (roundtrip input))
  (let [input "2r101010"]
    (valid? input)
    (roundtrip input))
  (let [input "8r52"]
    (valid? input)
    (roundtrip input))
  (let [input "36r16"]
    (valid? input)
    (roundtrip input))
  (let [input "22/7"]
    (valid? input)
    (roundtrip input)))


(deftest metadata
  (let [input "^String [a b 2]"]
    (valid? input)
    (roundtrip input))
  (let [input "^\"String\" [a b 2]"]
    (valid? input)
    (roundtrip input))
  (let [input "^:string [a b 2]"]
    (valid? input)
    (roundtrip input))
  (let [input "^{:a 1} [a b 2]"]
    (valid? input)
    (roundtrip input))
  (let [input "^:hello ^\"World\" ^{:a 1} [a b 2]"]
    (valid? input)
    (roundtrip input))
  ;; DEPRECATED meta data macro style
  (let [input "(meta #^{:a 10} #^String {})"]
    (valid? input)
    (roundtrip input))
  (let [input "^
        #_ :a
        {:a true}
        ;; hello
        #^ #_ hello :world
        [:a]"]
    (valid? input)
    (roundtrip input))
  (let [input "^ ::hello 'world"]
    (valid? input)
    (roundtrip input))
  (let [input "^ ::hello `first"]
    (valid? input)
    (roundtrip input))
  (let [input "^ ::hello #'first"]
    (valid? input)
    (roundtrip input))
  (let [input "(meta ^::hello #?(:clj {}))"]
    (valid? input)
    (roundtrip input))
  (let [input "(meta ^::hello #?@(:clj ({})))"]
    (valid? input)
    (roundtrip input))
  (let [input "(meta ^::hello @(atom {}))"]
    (valid? input)
    (roundtrip input))
  (let [input "(meta ^#?(:clj ::hello :cljs ::world) {})"]
    (valid? input)
    (roundtrip input)))


(deftest discard
  (let [input "#_[a b 2]"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "#_(a b 2)"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "#_{:a 1}"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "#_macros"]
    (valid? input)
    (roundtrip input))
  ;; discard statements can be "nested"
  (let [input "#_#_:a :b"]
    (valid? input)
    (roundtrip input))
  (let [input "#_#_ :a"]
    (is (parcera/failure? (parcera/ast input))))
  (let [input "#_
                  ;; hello
                  #_
                      ;; world
                      :hello
                  true
              ;; hey
              \"jo\""]
    (valid? input)
    (roundtrip input)))


(deftest regex
  (let [input "#_\"[a b 2]\""]
    (valid? input)
    (roundtrip input)))


(deftest comments
  (let [input "{:hello ;2}
                 2}"]
    (valid? input)
    (roundtrip input))
  (let [input ";[a b 2]"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input ";; \"[a b 2]\""]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "2 ;[a b 2]"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input " :hello ;; \"[a b 2]\""]
    (valid? input)
    (roundtrip input))
  (let [input " :hello #! \"[a b 2]\""]
    (valid? input)
    (roundtrip input))
  (let [input "#! invalid { input '"]
    (valid? input)
    (roundtrip input))
  (let [input "^{:a true} ;; hello
                          #_ :hello
                          [:a]"]
    (valid? input)
    (roundtrip input)))


(deftest var-quote
  (let [input "#'hello/world"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "#'/"]
    (valid? input)
    (roundtrip input))
  (let [input "#' #_ :hello str"]
    (valid? input)
    (roundtrip input)))


(deftest tag
  (let [input "#hello/world [1 a \"3\"]"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "#hello/world {1 \"3\"}"]
    (valid? input)
    (roundtrip input)))


(deftest quote
  (let [input "'hello/world"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "'hello"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "'/"]
    (valid? input)
    (roundtrip input))
  (let [input "' #_ :hello (str \"world\")"]
    (valid? input)
    (roundtrip input)))


(deftest backtick
  (let [input "`hello/world"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "`hello"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "`/"]
    (valid? input)
    (roundtrip input))
  (let [input "` #_ :hello (str \"world\")"]
    (valid? input)
    (roundtrip input)))


(deftest unquote-macro
  (let [input "~hello/world"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "~(hello 2 3)"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "~/"]
    (valid? input)
    (roundtrip input)))


(deftest quote-splicing
  (let [input "~@hello/world"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "~@(hello 2 b)"]
    (valid? input)
    (roundtrip input)))


(deftest deref-macro
  (let [input "@hello/world"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "@hello"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "@/"]
    (valid? input)
    (roundtrip input)))


(deftest anonymous-function
  (let [input "#(= (str %1 %2 %&))"]
    (valid? input)
    (roundtrip input)))


(deftest namespaced-map
  (let [input "#::{:a 1 b 3}"]
    (valid? input)
    (roundtrip input))
  ;(is (clear input))))
  (let [input "#::hello{:a 1 b 3}"]
    (valid? input)
    (roundtrip input))
  (let [input "::platform #_ :world #_rofl {:hello true}"]
    (valid? input)
    (roundtrip input)))


(deftest reader-conditional-macro
  (let [input "#?(:clj Double/NaN :cljs js/NaN :default nil)"]
    (is (valid? input))
    (is (roundtrip input)))
  (let [input "[1 2 #?@(:clj [3 4] :cljs [5 6])]"]
    (is (valid? input))
    (is (roundtrip input)))
  (let [input "#? ;; hello
    (
     :clj  (println \"hello\")
     :cljs (println \"world\"))"]
    (is (parcera/failure? (parcera/ast input))))
  (let [input "#?   (
     :clj  (println \"hello\")
     :cljs (println \"world\"))"]
    (is (valid? input))
    (is (roundtrip input))))

(deftest whitespace
  (let [input "(defmacro x [a] `   #'  ~  '  a)"]
    (valid? input)
    (roundtrip input)))


(deftest eval-macro
  (let [input "#=  (inc 1)"]
    (valid? input)
    (roundtrip input))
  (let [input "#=inc"]
    (valid? input)
    (roundtrip input))
  (let [input "#= #_true #_ :hello (println 3)"]
    (valid? input)
    (roundtrip input)))


(deftest symbolic
  (let [input "##Inf"]
    (valid? input)
    (roundtrip input))
  (let [input "##-Inf"]
    (valid? input)
    (roundtrip input))
  ;; symbolic names are valid symbols
  (let [input "Inf"]
    (valid? input)
    (roundtrip input))
  (let [input "## #_ hello
                  ;; world
                  Inf"]
    (valid? input)
    (is (= input (parcera/code (parcera/ast input))))))


(deftest EOF
  (let [input ":hello \"  "]
    (is (parcera/failure? (parcera/ast input)))))
;(is (clear input))))))


(deftest bootstrap

  (testing "parcera should be able to parse itself"
    (let [input (slurp "./src/clojure/parcera/core.cljc")]
      (valid? input)
      (roundtrip input)))

  (testing "parcera should be able to parse its own test suite"
    (let [input (slurp "./test/parcera/test_cases.cljc")]
      (valid? input)
      (roundtrip input))
    (let [input (slurp "./test/parcera/benchmark.clj")]
      (valid? input)
      (roundtrip input))
    (let [input (slurp "./test/parcera/slurp.cljc")]
      (valid? input)
      (roundtrip input))))

(defonce clojure (slurp "https://raw.githubusercontent.com/clojure/clojure/master/src/clj/clojure/core.clj"))
(defonce clojure$script (slurp "https://raw.githubusercontent.com/clojure/clojurescript/master/src/main/clojure/cljs/core.cljc"))

(deftest clojure$cript-bootstrap

  (testing "parcera should be able to parse clojure core"
    (let [code (parcera/ast clojure)]
      (time (is (= clojure (parcera/code code))))
      (is (not (parcera/failure? code)))))

  (testing "parcera should be able to parse clojurescript core"
    (let [code (parcera/ast clojure$script)]
      (time (is (= clojure$script (parcera/code code))))
      (is (not (parcera/failure? code))))))

;; when in doubt enable the test below. I parses clojure reader test suite so, if we
;; expect something to work it probably will be tested there.
#_(testing "parcera should be able to parse clojurescript core"
    (let [core-content (slurp "https://raw.githubusercontent.com/clojure/clojure/master/test/clojure/test_clojure/reader.cljc")]
      (for [form (tree-seq seq? seq (parcera/ast core-content :optimize :memory))
            :when (coll? form)
            :when (contains? #{::parcera/failure} (first form))]
        [form (meta form)])))
